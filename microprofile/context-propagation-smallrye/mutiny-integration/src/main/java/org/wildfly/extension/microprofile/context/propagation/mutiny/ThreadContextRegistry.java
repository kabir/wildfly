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

package org.wildfly.extension.microprofile.context.propagation.mutiny;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.context.spi.ContextManagerProvider;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ThreadContextRegistry {
    public static ThreadContextRegistry INSTANCE = new ThreadContextRegistry();

    /**
     * Guarded by 'this'
     */
    private final Map<ClassLoader, ThreadContext> threadContextsByClassLoader = Collections.synchronizedMap(new HashMap<>());

    private ThreadContextRegistry() {
    }

    ThreadContext getThreadContext() {
        return getThreadContext(Thread.currentThread().getContextClassLoader());
    }

    ThreadContext getThreadContext(ClassLoader cl) {
        ThreadContext tc = threadContextsByClassLoader.get(cl);
        if (tc == null) {
            synchronized (this) {
                tc = threadContextsByClassLoader.get(cl);
                if (tc == null) {
                    tc = ContextManagerProvider.instance()
                            .getContextManager(cl)
                            .newThreadContextBuilder()
                            .build();
                    threadContextsByClassLoader.put(cl, tc);
                }
            }
        }
        return tc;
    }

    public void cleanup(ClassLoader cl) {
        threadContextsByClassLoader.remove(cl);
    }
}
