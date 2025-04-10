/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye.cdi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.inject.ConfigProducer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

public class MicroprofileConfigEarClassLoaderCdiExtension implements Extension {
    private ClassLoader classLoader;

    public MicroprofileConfigEarClassLoaderCdiExtension(ClassLoader classLoader) {
        this.classLoader = classLoader;
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

    public void registerConfigProducerBeans(@Observes AfterBeanDiscovery event, BeanManager beanManager) {
        System.out.println("Adding bean for " + beanManager);
        event.addBean().addType(Config.class).addTransitiveTypeClosure(SmallRyeConfig.class)
                .scope(ApplicationScoped.class)
                .addQualifier(Default.Literal.INSTANCE)
                .produceWith(instance -> ConfigProvider.getConfig(classLoader).unwrap(SmallRyeConfig.class));

    }
}
