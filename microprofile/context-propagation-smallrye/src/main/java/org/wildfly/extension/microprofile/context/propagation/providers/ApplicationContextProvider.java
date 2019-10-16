/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.context.propagation.providers;

import java.security.PrivilegedAction;
import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ApplicationContextProvider implements ThreadContextProvider {
    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> props) {
        return ApplicationThreadContextSnapshot.create(true);
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> props) {
        return ApplicationThreadContextSnapshot.create(false);
    }

    @Override
    public String getThreadContextType() {
        return ThreadContext.APPLICATION;
    }

    private static class ApplicationThreadContextSnapshot implements ThreadContextSnapshot {
        final ClassLoader tccl;
        final boolean propagate;

        private ApplicationThreadContextSnapshot(ClassLoader tccl, boolean propagate) {
            this.tccl = tccl;
            this.propagate = propagate;
        }

        static ApplicationThreadContextSnapshot create(boolean propagate) {
            final ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            return new ApplicationThreadContextSnapshot(tccl, propagate);
        }

        @Override
        public ThreadContextController begin() {
            if (propagate) {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
            } else {
                ClassLoader system = WildFlySecurityManager.doChecked(GetSystemClassLoaderAction.INSTANCE);
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(system);
            }
            return new ThreadContextController() {
                @Override
                public void endContext() throws IllegalStateException {
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
