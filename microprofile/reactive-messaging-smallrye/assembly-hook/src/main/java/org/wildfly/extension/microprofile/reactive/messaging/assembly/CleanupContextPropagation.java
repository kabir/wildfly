/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.microprofile.reactive.messaging.assembly;

import java.util.List;
import java.util.concurrent.Executor;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.context.ThreadContext;

import io.smallrye.reactive.messaging.ChannelRegistar;
import io.smallrye.reactive.messaging.ChannelRegistry;
import io.smallrye.reactive.messaging.assembly.AssemblyHook;

@ApplicationScoped
public class CleanupContextPropagation implements AssemblyHook {
    @Override
    public Executor before(List<ChannelRegistar> registars,
            ChannelRegistry registry) {

        try {
            CleanupContextPropagation.class.getClassLoader().loadClass("org.eclipse.microprofile.context.ThreadContext");
            return ThreadContext.builder()
                    .unchanged()
                    .propagated(ThreadContext.APPLICATION)
                    .cleared(ThreadContext.ALL_REMAINING)
                    .build()
                    .currentContextExecutor();
        } catch (Exception ignored) {
            // No context propagation.
            return null;
        }
    }
}
