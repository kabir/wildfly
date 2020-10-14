/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.test.integration.microprofile.context.propagation.rest;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/context")
@Produces(MediaType.TEXT_PLAIN)
public class ContextPropagationEndpoint {
    @Inject
    ManagedExecutor allExecutor;

    @Inject
    ThreadContext allTc;

    @Inject
    TransactionManager tm;

    @Inject
    ContextPropagationEndpoint thisBean;

    @GET
    @Path("/tccl")
    public CompletionStage<String> tcclTest() {
        ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            ClassLoader tccl2 = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            if (tccl != tccl2) {
                throw new IllegalStateException("TCCL was not the same");
            }
            return text;
        });
    }

    @GET
    @Path("/tccl-tc")
    public CompletionStage<String> tcclTcTest() {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));

        return ret.thenApplyAsync(text -> {
            ClassLoader tccl2 = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            if (tccl != tccl2) {
                throw new IllegalStateException("TCCL was not the same");
            }
            return text;
        }, executor);
    }

    @GET
    @Path("/tccl-rso")
    public Publisher<String> tcclRsoJavaTest() {
        ClassLoader tccl = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        return ReactiveStreams.of("OK")
                .map(v -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return v;
                })
                .map(text -> {
                    ClassLoader tccl2 = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                    if (tccl != tccl2) {
                        throw new IllegalStateException("TCCL was not the same");
                    }
                    return text;
                })
                .buildRs();
    }

    @GET
    @Path("/resteasy")
    public CompletionStage<String> resteasyTest(@Context UriInfo uriInfo) {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            uriInfo.getAbsolutePath();
            return text;
        });
    }

    @GET
    @Path("/resteasy-tc")
    public CompletionStage<String> resteasyTcTest(@Context UriInfo uriInfo) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));

        return ret.thenApplyAsync(text -> {
            uriInfo.getAbsolutePath();
            return text;
        }, executor);
    }

    @GET
    @Path("/resteasy-rso")
    public Publisher<String> resteasyRsoTest(@Context UriInfo uriInfo) {
        return ReactiveStreams.of("OK")
                .map(v -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return v;
                })
                .map(text -> {
                    uriInfo.getAbsolutePath();
                    return text;
                })
                .buildRs();
    }

    @GET
    @Path("/servlet")
    public CompletionStage<String> servletTest(@Context HttpServletRequest servletRequest) {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            servletRequest.getContentType();
            return text;
        });
    }

    @GET
    @Path("/servlet-tc")
    public CompletionStage<String> servletTcTest(@Context HttpServletRequest servletRequest) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));

        return ret.thenApplyAsync(text -> {
            servletRequest.getContentType();
            return text;
        }, executor);
    }

    @GET
    @Path("/servlet-rso")
    public Publisher<String> servletRsoTest(@Context HttpServletRequest servletRequest) {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        return ReactiveStreams.of("OK")
                .map(v -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return v;
                })
                .map(text -> {
                    servletRequest.getContentType();
                    return text;
                })
                .buildRs();
    }

    @GET
    @Path("/cdi")
    public CompletionStage<String> cdiTest() {
        RequestBean instance = getRequestBean();
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = getRequestBean();
            if (instance.id() != instance2.id()) {
                throw new IllegalStateException("Instances were not the same");
            }
            return text;
        });
    }

    @GET
    @Path("/cdi-tc")
    public CompletionStage<String> cdiTcTest() {
        RequestBean instance = getRequestBean();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));

        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = getRequestBean();
            if (instance.id() != instance2.id()) {
                throw new IllegalStateException("Instances were not the same");
            }
            return text;
        }, executor);
    }

    @GET
    @Path("/cdi-rso")
    public Publisher<String> cdiRsoTest() {
        RequestBean instance = getRequestBean();

        return ReactiveStreams.of("OK")
                .map(v -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return v;
                })
                .map(text -> {
                    RequestBean instance2 = getRequestBean();
                    if (instance.id() != instance2.id()) {
                        throw new IllegalStateException("Instances were not the same");
                    }
                    return text;
                })
                .buildRs();
    }

    @GET
    @Path("/nocdi")
    public CompletionStage<String> noCdiTest() {
        ManagedExecutor me = ManagedExecutor.builder().cleared(ThreadContext.CDI).build();
        RequestBean instance = getRequestBean();
        long id = instance.id();
        CompletableFuture<String> ret = me.completedFuture("OK");
        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = getRequestBean();

            if (id == instance2.id()) {
                throw new IllegalStateException("Instances were the same");
            }
            return text;
        });
    }


    @GET
    @Path("/nocdi-tc")
    public CompletionStage<String> noCdiTcTest() {
        ThreadContext tc = ThreadContext.builder().cleared(ThreadContext.CDI).build();
        RequestBean instance = getRequestBean();
        long id = instance.id();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = tc.withContextCapture(CompletableFuture.completedFuture("OK"));

        return ret.thenApplyAsync(text -> {
            RequestBean instance2 = getRequestBean();

            if (id == instance2.id()) {
                throw new IllegalStateException("Instances were the same");
            }
            return text;
        }, executor);
    }

    @GET
    @Path("/nocdi-rso")
    public Publisher<String> noRsoJavaTest() {
        throw new IllegalStateException("Not possible to clear contexts");
    }


    @Transactional
    @GET
    @Path("/transaction")
    public CompletionStage<String> transactionalTest() throws SystemException {
        return asyncTransactional();
    }

    @Transactional
    @GET
    @Path("/transactionnew")
    public CompletionStage<String> transactionalTestNew() throws SystemException {
        Transaction t1 = tm.getTransaction();
        return thisBean.asyncTransactionalRequiresNew(t1)
                .thenComposeAsync(f -> {
                    try {
                        // here we expect that the requires new has started and suspended its own transaction
                        if (!t1.equals(tm.getTransaction())) {
                            throw new IllegalStateException("Expecting transaction being the same as for the @GET method");
                        }
                        return asyncTransactional();
                    } catch (SystemException se) {
                        throw new IllegalStateException("Cannot get state of transaction", se);
                    }
                });
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public CompletionStage<String> asyncTransactionalRequiresNew(Transaction originalTransaction) throws SystemException {
        if (originalTransaction.equals(tm.getTransaction())) {
            throw new IllegalStateException("Expecting a new transaction being started and being different from the provided one");
        }
        return asyncTransactional();
    }

    private CompletionStage<String> asyncTransactional() throws SystemException {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        final Transaction t1 = tm.getTransaction();
        if (t1 == null) {
            throw new IllegalStateException("No TM");
        }

        return ret.thenApplyAsync(text -> {
            Transaction t2;
            try {
                t2 = tm.getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            if (!t1.equals(t2)) {
                throw new IllegalStateException("Different transactions");
            }
            try {
                int txnStatus = t1.getStatus();
                if (t1.getStatus() != Status.STATUS_ACTIVE) {
                    throw new IllegalStateException("Expecting the transaction being active");
                }
            } catch (SystemException se) {
                throw new IllegalStateException("Cannot get transaction status", se);
            }

            return text;
        });
    }

    private RequestBean getRequestBean() {
        BeanManager manager = CDI.current().getBeanManager();
        return manager.createInstance().select(RequestBean.class).get();
    }
}
