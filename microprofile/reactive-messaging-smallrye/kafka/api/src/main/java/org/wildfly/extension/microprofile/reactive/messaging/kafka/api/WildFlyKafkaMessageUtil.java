/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.microprofile.reactive.messaging.kafka.api;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Message;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class WildFlyKafkaMessageUtil {
    private static final String DELEGATE_CLASS = "org.wildfly.extension.microprofile.reactive.messaging.kafka.bridge.InternalUtilImpl";
    private volatile InternalUtil delegate;

    protected WildFlyKafkaMessageUtil() {

    }

    @PostConstruct
    private void postConstruct() {
        try {
            Class<? extends InternalUtil> clazz = loadDelegateClass();
            delegate = clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        }
    }

    private Class<? extends InternalUtil> loadDelegateClass() throws Exception {
        Class<? extends InternalUtil> clazz;
        if (System.getSecurityManager() == null) {
            clazz = this.getClass().getClassLoader().loadClass(DELEGATE_CLASS).asSubclass(InternalUtil.class);
        } else {
            try {
                clazz = AccessController.doPrivileged(new PrivilegedExceptionAction<Class<? extends InternalUtil>>() {
                    @Override
                    public Class<? extends InternalUtil> run() throws Exception {
                        return this.getClass().getClassLoader().loadClass(DELEGATE_CLASS).asSubclass(InternalUtil.class);
                    }
                });
            } catch (PrivilegedActionException e) {
                Throwable t = e.getCause();
                if (t instanceof Exception) {
                    throw (Exception) t;
                }
                throw new Exception(t);
            }
        }
        return clazz;
    }


    @PreDestroy
    private void preDestroy() {
        delegate = null;
    }

    public <K, T> WildFlyKafkaMessage<K, T> from(Message<T> message) {
        return delegate.from(message);
    }
}
