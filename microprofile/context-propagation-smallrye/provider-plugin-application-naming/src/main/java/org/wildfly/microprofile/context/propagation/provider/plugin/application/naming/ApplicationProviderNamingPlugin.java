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

package org.wildfly.microprofile.context.propagation.provider.plugin.application.naming;

import java.util.Map;

import org.eclipse.microprofile.context.spi.ThreadContextController;
import org.eclipse.microprofile.context.spi.ThreadContextSnapshot;
import org.jboss.as.naming.context.NamespaceContextSelector;
import org.wildfly.microprofile.context.propagation.provider.plugin.ThreadContextProviderPlugin;

/**
 * A plugin for the Application ThreadContextProvider that handles
 * propagation of the Jakarta EE Naming Context. This will only
 * be active if the naming subsystem is present
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ApplicationProviderNamingPlugin implements ThreadContextProviderPlugin {

    @Override
    public ThreadContextSnapshot currentContext(Map<String, String> map) {
        return new NamingThreadContextSnapshot(true);
    }

    @Override
    public ThreadContextSnapshot clearedContext(Map<String, String> map) {
        return new NamingThreadContextSnapshot(false);
    }

    private class NamingThreadContextSnapshot implements ThreadContextSnapshot {
        private boolean propagate;
        private final NamespaceContextSelector current;

        NamingThreadContextSnapshot(boolean propagate) {
            this.propagate = propagate;
            current = NamespaceContextSelector.getCurrentSelector();
        }

        @Override
        public ThreadContextController begin() {
            if (propagate) {
                NamespaceContextSelector.pushCurrentSelector(current);
            } else {
                NamespaceContextSelector.pushCurrentSelector(null);
            }
            return new ThreadContextController() {
                @Override
                public void endContext() throws IllegalStateException {
                    NamespaceContextSelector.popCurrentSelector();
                }
            };
        }
    }
}
