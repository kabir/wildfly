/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye.cdi;

import java.util.HashSet;
import java.util.Set;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.InjectionPoint;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.inject.ConfigProducer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.wildfly.security.manager.WildFlySecurityManager;

public class MicroprofileConfigEarClassLoaderCdiExtension implements Extension {
    private final Set<ClassLoader> classLoaders = new HashSet<>();

    public MicroprofileConfigEarClassLoaderCdiExtension() {
    }

    public void addClassLoader(ClassLoader classLoader) {
        classLoaders.add(classLoader);
    }

    public void veto(@Observes ProcessAnnotatedType<ConfigProducer> event, BeanManager beanManager) {
        // Don't install the ConfigProducer
        if (event.getAnnotatedType().getJavaClass().equals(ConfigProducer.class)) {
            System.out.println("Vetoing bean for " + beanManager);
            // Disable for now since it breaks everything
            event.veto();
        }
    }

//    public void registerConfigProducerAnnotatedType(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
//        event.addAnnotatedType(EarConfigProducer.class, EarConfigProducer.class.getName() + "-override");
//    }

    public void registerConfigProducerBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        System.out.println("Adding bean for " + beanManager);
        abd.addBean().scope(Dependent.class)
//                .alternative(true)
//                .priority(10)
                .beanClass(SmallRyeConfig.class)
                .addTypes(SmallRyeConfig.class, Config.class)
                .produceWith(instance -> {
                    InjectionPoint ip = instance.select(InjectionPoint.class).get();
                    System.out.println("---> IP: " + ip);
                    System.out.println("---> Bean: " + ip.getBean());
                    ClassLoader cl = null;
                    if (ip.getBean() != null) {
                        ClassLoader test = ip.getBean().getBeanClass().getClassLoader(); // With security manager
                        if (classLoaders.contains(test)) {
                            cl = test;
                        }
                    }
                    if (cl == null) {
                        cl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                    }
                    return ConfigProvider.getConfig(cl).unwrap(SmallRyeConfig.class);
                });
    }
}
