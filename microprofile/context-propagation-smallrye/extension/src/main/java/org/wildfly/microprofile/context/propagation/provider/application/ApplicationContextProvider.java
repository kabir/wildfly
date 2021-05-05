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

package org.wildfly.microprofile.context.propagation.provider.application;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.wildfly.microprofile.context.propagation.provider.plugin.ThreadContextProviderPlugin;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ApplicationContextProvider implements ThreadContextProvider {

    private final List<ThreadContextProviderPlugin> providerPlugins;

    public ApplicationContextProvider() {
        List<ThreadContextProviderPlugin> plugins = new ArrayList<>();
        ServiceLoader<ThreadContextProviderPlugin> loader = ServiceLoader.load(ThreadContextProviderPlugin.class);
        for (Iterator<ThreadContextProviderPlugin> it = loader.iterator(); it.hasNext(); ) {
            plugins.add(it.next());
        }
        this.providerPlugins = plugins;
    }

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return ApplicationThreadContextSnapshot.create(true, providerPlugins, props);
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return ApplicationThreadContextSnapshot.create(false, providerPlugins, props);
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.APPLICATION;
    }

    private static class ApplicationThreadContextSnapshot implements ThreadContextSnapshot {
        final ClassLoader tccl;
        final boolean propagate;
        private List<ThreadContextProviderPlugin> providerPlugins;
        private Map<String, String> props;

        private ApplicationThreadContextSnapshot(
                ClassLoader tccl, boolean propagate, List<ThreadContextProviderPlugin> providerPlugins, Map<String, String> props) {
            this.tccl = tccl;
            this.propagate = propagate;
            this.providerPlugins = providerPlugins;
            this.props = props;
        }

        static ApplicationThreadContextSnapshot create(
                boolean propagate, List<ThreadContextProviderPlugin> providerPlugins, Map<String, String> props) {
            final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            return new ApplicationThreadContextSnapshot(tccl, propagate, providerPlugins, props);
        }

        @Override
        public ThreadContextController begin() {
            if (propagate) {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
            } else {
                ClassLoader system = WildFlySecurityManager.doChecked(GetSystemClassLoaderAction.INSTANCE);
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(system);
            }
            List<ThreadContextController> pluginControllers;
            if (providerPlugins.size() > 0) {
                pluginControllers = new ArrayList<>();
                for (ThreadContextProviderPlugin plugin : providerPlugins) {
                    if (propagate) {
                        pluginControllers.add(plugin.currentContext(props).begin());
                    } else {
                        pluginControllers.add(plugin.clearedContext(props).begin());
                    }
                }
            } else {
                pluginControllers = Collections.emptyList();
            }
            return new ThreadContextController() {
                @Override
                public void endContext() throws IllegalStateException {
                    for (ThreadContextController pluginController : pluginControllers) {
                        pluginController.endContext();
                    }
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
                }
            };
        }
    }

    private static class GetSystemClassLoaderAction implements PrivilegedAction<ClassLoader> {
        static final GetSystemClassLoaderAction INSTANCE = new GetSystemClassLoaderAction();
        @Override
        public ClassLoader run() {
            return ClassLoader.getSystemClassLoader();
        }
    }

}
