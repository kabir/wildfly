/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye.cdi;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import io.smallrye.config.inject.ConfigProducer;

public class MicroprofileConfigEarClassLoaderCdiExtension implements Extension {
    private ClassLoader classLoader;

    public MicroprofileConfigEarClassLoaderCdiExtension(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public void veto(@Observes ProcessAnnotatedType<?> event) {
        // Don't install the ConfigProducer
        if (event.getAnnotatedType().getJavaClass().equals(ConfigProducer.class)) {
            // Disable for now since it breaks everything
            //event.veto();
        }
    }

    public void registerConfigProducerAnnotatedType(@Observes BeforeBeanDiscovery event, BeanManager beanManager) {
        event.addAnnotatedType(EarConfigProducer.class, EarConfigProducer.class.getName() + "-override");
    }

}
