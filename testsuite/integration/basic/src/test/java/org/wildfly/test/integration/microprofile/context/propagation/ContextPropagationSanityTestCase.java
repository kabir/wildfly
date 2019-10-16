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

package org.wildfly.test.integration.microprofile.context.propagation;

import java.security.PrivilegedAction;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
public class ContextPropagationSanityTestCase {

    @Inject
    @TestQualifier
    ManagedExecutor managedExecutor;

    @Inject
    @TestQualifier
    ThreadContext threadContext;


    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "ctx-ppgn.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ContextPropagationSanityTestCase.class.getPackage());
        return webArchive;
    }

    @Test
    public void testCanLoadManagedExecutor() {
        ManagedExecutor executor = ManagedExecutor.builder().build();
        executor.shutdownNow();
    }

    @Test
    public void testCanLoadThreadContext() {
        ThreadContext threadContext = ThreadContext.builder().build();
    }

    @Test
    public void testManagedExecutorWasInjected() {
        Assert.assertNotNull(managedExecutor);
    }

    @Test
    public void testThreadContextWasInjected() {
        Assert.assertNotNull(threadContext);
    }

    @Test
    public void testClearedApplicationThreadContext() throws Exception {
        ClassLoader originalTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();

        ThreadContext context = ThreadContext.builder()
                .cleared(ThreadContext.APPLICATION)
                .build();

        Callable<ClassLoader> task = context.contextualCallable(() -> {
            return WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        });

        ClassLoader cl = task.call();
        ClassLoader system = getSystemClassloader();
        Assert.assertSame(system, cl);

        Assert.assertSame(originalTccl, WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Test
    public void testPropagatedApplicationThreadContext() throws Exception {
        ClassLoader originalTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();

        ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        ThreadContext context = ThreadContext.builder()
                .propagated(ThreadContext.APPLICATION)
                .build();

        Callable<ClassLoader> task = context.contextualCallable(() -> {
            return WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        });

        ClassLoader cl = task.call();
        Assert.assertSame(tccl, cl);

        Assert.assertSame(originalTccl, WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Test
    public void testUnchangedApplicationThreadContext() throws Exception {
        ClassLoader originalTccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();

        ThreadContext context = ThreadContext.builder()
                .unchanged(ThreadContext.APPLICATION)
                .build();

        Callable<ClassLoader> task = context.contextualCallable(() -> {
            return WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        });

        ClassLoader cl = task.call();
        // This is the same because we don't actually change the thread here
        Assert.assertSame(originalTccl, cl);
        Assert.assertSame(originalTccl, WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());
    }

    @Test
    public void testClearedApplicationManagedExecutor() throws Exception {
        TcclBackup tcclBackup = new TcclBackup();
        try {
            ManagedExecutor executor = ManagedExecutor.builder()
                    .cleared(ThreadContext.APPLICATION)
                    .build();

            // Set up a classloader that is not used elsewhere as the TCCL
            ClassLoader testTccl = new ClassLoader() {
            };
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(testTccl);


            CompletableFuture<ClassLoader> future = executor.supplyAsync(() -> {
                return WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            });


            ClassLoader cl = future.get();
            ClassLoader system = getSystemClassloader();
            Assert.assertSame(system, cl);

            Assert.assertSame(testTccl, WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());

        } finally {
            tcclBackup.restore();
        }
    }

    @Test
    public void testPropagatedApplicationManagedExecutor() throws Exception {
        TcclBackup tcclBackup = new TcclBackup();
        try {
            ManagedExecutor executor = ManagedExecutor.builder()
                    .propagated(ThreadContext.APPLICATION)
                    .build();

            // Set up a classloader that is not used elsewhere as the TCCL
            ClassLoader testTccl = new ClassLoader() {
            };
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(testTccl);


            CompletableFuture<ClassLoader> future = executor.supplyAsync(() -> {
                return WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            });


            ClassLoader cl = future.get();
            Assert.assertSame(testTccl, cl);

            Assert.assertSame(testTccl, WildFlySecurityManager.getCurrentContextClassLoaderPrivileged());

        } finally {
            tcclBackup.restore();
        }
    }

    private ClassLoader getSystemClassloader() {
        return WildFlySecurityManager.doChecked(GetSystemClassLoaderAction.INSTANCE);
    }


    private static class GetSystemClassLoaderAction implements PrivilegedAction<ClassLoader> {
        static final GetSystemClassLoaderAction INSTANCE = new GetSystemClassLoaderAction();
        @Override
        public ClassLoader run() {
            return ClassLoader.getSystemClassLoader();
        }
    }

    private static class TcclBackup {
        private final ClassLoader tccl;

        TcclBackup() {
            this.tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        }

        void restore() {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(tccl);
        }
    }
}

