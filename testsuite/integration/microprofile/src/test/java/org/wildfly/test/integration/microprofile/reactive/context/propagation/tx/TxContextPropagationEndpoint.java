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

package org.wildfly.test.integration.microprofile.reactive.context.propagation.tx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.jboss.resteasy.annotations.Stream;
import org.reactivestreams.Publisher;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/context")
@Produces(MediaType.TEXT_PLAIN)
@RequestScoped
public class TxContextPropagationEndpoint {
    @Inject
    ManagedExecutor allExecutor;

    @Inject
    ThreadContext allTc;

    @PersistenceContext(unitName = "test")
    EntityManager em;

    @Inject
    TransactionManager tm;

    @Inject
    TransactionalBean txBean;

    @Transactional
    @GET
    @Path("/transaction1")
    public CompletionStage<String> transactionTest1() throws SystemException {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");

        ContextEntity entity = new ContextEntity();
        entity.setName("KK");
        em.persist(entity);
        Transaction t1 = tm.getTransaction();
        TestUtils.assertNotNull("No tx", t1);
        TestUtils.assertEquals(1, TestUtils.count(em));

        return ret.thenApplyAsync(text -> {
            Transaction t2;
            try {
                t2 = tm.getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            TestUtils.assertSame(t1, t2);
            TestUtils.assertEquals(1, TestUtils.count(em));
            return text;
        });
    }

    @Transactional
    @GET
    @Path("/transaction2")
    public CompletionStage<String> transactionTest2() {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");
        // We should have the one entry from transactionTest1
        TestUtils.assertEquals(1, TestUtils.count(em));
        TestUtils.assertEquals(1, TestUtils.deleteAll(em));
        return ret.thenApplyAsync(x -> {
            // Save entity
            ContextEntity entity = new ContextEntity();
            entity.setName("KK");
            em.persist(entity);
            // Exception to rollback
            throw new WebApplicationException(Response.status(Response.Status.CONFLICT).build());
        });
    }

    @Transactional
    @GET
    @Path("/transaction3")
    public Publisher<String> transactionTest3() {
        // The attempt at saving an entry asynchronously in /transaction2 should not have been saved
        TestUtils.assertEquals(1, TestUtils.count(em));
        TestUtils.assertEquals(1, TestUtils.deleteAll(em));
        return ReactiveStreams.of("OK")
                .map((Function<String, String>) s -> {
                    // Save entity
                    ContextEntity entity = new ContextEntity();
                    entity.setName("KK");
                    em.persist(entity);
                    // Exception to rollback
                    throw new WebApplicationException(Response.status(Response.Status.CONFLICT).build());
                }).buildRs();
    }

    @Transactional
    @GET
    @Path("/transaction-tc")
    public CompletionStage<String> transactionThreadContextTest() throws SystemException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));

        ContextEntity entity = new ContextEntity();
        entity.setName("KK");
        em.persist(entity);

        Transaction t1 = tm.getTransaction();
        TestUtils.assertNotNull("No tx", t1);

        return ret.thenApplyAsync(text -> {
            TestUtils.assertEquals(1, TestUtils.count(em));
            Transaction t2;
            try {
                t2 = tm.getTransaction();
            } catch (SystemException e) {
                throw new RuntimeException(e);
            }
            TestUtils.assertEquals(t1, t2);

            return text;
        }, executor);
    }

    @Transactional
    @GET
    @Path("/transaction-new")
    public CompletionStage<String> transactionNewTest() throws SystemException {
        CompletableFuture<String> ret = allExecutor.completedFuture("OK");

        Transaction t1 = tm.getTransaction();
        TestUtils.assertNotNull("No tx", t1);

        txBean.doInTx();

        // We should see the transaction already committed even if we're async
        TestUtils.assertEquals(1, TestUtils.deleteAll(em));
        TestUtils.assertEquals(0, TestUtils.deleteAll(em));
        return ret;
    }


    @Transactional
    @GET
    @Path("/transaction-rso-publisher")
    @Stream(value = Stream.MODE.RAW)
    public Publisher<String> transactionRsoPublisher() throws SystemException {
        ContextEntity entity = new ContextEntity();
        entity.setName("KK");
        em.persist(entity);

        Transaction t1 = tm.getTransaction();
        TestUtils.assertNotNull("No tx", t1);

        // our entity
        TestUtils.assertEquals(1, TestUtils.count(em));

        return txBean.doInTxRsoPublisher()
                .map(v -> {
                    try {
                        // RSO doesn't have a proper delay() as RxJava does, so add our own 'dodgy' version of a delay here
                        // This is to make sure the main call thread has completed by the time the Tx is ended
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return v;
                })
                .map(text -> {
                    Transaction t2;
                    try {
                        t2 = tm.getTransaction();
                    } catch (SystemException e) {
                        throw new RuntimeException(e);
                    }
                    TestUtils.assertEquals(t1, t2);
                    int status2;
                    try {
                        status2 = t2.getStatus();
                    } catch (SystemException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    TestUtils.assertEquals(Status.STATUS_ACTIVE, status2);
                    return text;
                }).buildRs();
    }

    @Transactional
    @GET
    @Path("/transaction-propagated-tc")
    public CompletionStage<String> transactionPropagatedToThreadContextCompletionStage() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CompletableFuture<String> ret = allTc.withContextCapture(CompletableFuture.completedFuture("OK"));

        ContextEntity entity = new ContextEntity();
        entity.setName("KK");
        em.persist(entity);

        return ret.thenApplyAsync(text -> {
            ContextEntity entity2 = new ContextEntity();
            entity2.setName("KK");
            em.persist(entity2);

            return text;
        }, executor);
    }

    @Transactional
    @GET
    @Path("/transaction-propagated-exec")
    public CompletionStage<String> transactionPropagatedToManagedExecutorCompletionStage() {
        CompletionStage<String> cs = allExecutor.supplyAsync(() -> {
            ContextEntity entity2 = new ContextEntity();
            entity2.setName("KK");
            em.persist(entity2);
            return "OK";
        });

        ContextEntity entity = new ContextEntity();
        entity.setName("KK");
        em.persist(entity);
        return cs;
    }

    @Transactional
    @GET
    @Path("/transaction-propagated-publisher")
    @Stream(value = Stream.MODE.RAW)
    public Publisher<String> transactionPropagatedToPublisher() {
        ContextEntity entity = new ContextEntity();
        entity.setName("KK");
        em.persist(entity);

        return ReactiveStreams.of("OK")
                .map(v -> {
                    try {
                        // RSO doesn't have a proper delay() as RxJava does, so add our own 'dodgy' version of a delay here
                        // This is to make sure the main call thread has completed by the time the Tx is ended
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return v;
                })
                .map(v -> {
                    ContextEntity entity2 = new ContextEntity();
                    entity2.setName("KK");
                    em.persist(entity2);
                    return "OK";
                }).buildRs();
    }

    @Transactional
    @GET
    @Path("/transaction-propagated-cs-wrapped-in-publisher")
    @Stream(value = Stream.MODE.RAW)
    public Publisher<String> transactionPropagatedToManagedExecutorWrappedInPublisher() {
        ContextEntity entity = new ContextEntity();
        entity.setName("KK");
        em.persist(entity);

        CompletionStage<String> cs = allExecutor.supplyAsync(() -> {
            ContextEntity entity2 = new ContextEntity();
            entity2.setName("KK");
            em.persist(entity2);
            return "OK";
        });
        return ReactiveStreams.fromCompletionStage(cs)
                .map(v -> {
                    try {
                        // RSO doesn't have a proper delay() as RxJava does, so add our own 'dodgy' version of a delay here
                        // This is to make sure the main call thread has completed by the time the Tx is ended
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                    return v;
                })
                .map(v -> {
                    ContextEntity entity2 = new ContextEntity();
                    entity2.setName("KK");
                    em.persist(entity2);
                    return "OK";
                }).buildRs();
    }

    @Transactional
    @GET
    @Path("/transaction-delete1")
    public String transactionDelete1() {
        // now delete both entities
        TestUtils.assertEquals(1, TestUtils.deleteAll(em));
        return "OK";
    }

    @Transactional
    @GET
    @Path("/transaction-delete2")
    public String transactionDelete2() {
        // now delete both entities
        TestUtils.assertEquals(2, TestUtils.deleteAll(em));
        return "OK";
    }

    @Transactional
    @GET
    @Path("/transaction-delete3")
    public String transactionDelete3() {
        // now delete both entities
        TestUtils.assertEquals(3, TestUtils.deleteAll(em));
        return "OK";
    }

    @Transactional
    @GET
    @Path("/transaction-delete-all")
    public String transactionDeleteAll() {
        // now delete both entities
        TestUtils.deleteAll(em);
        return "OK";
    }
}
