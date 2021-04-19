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

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.wildfly.extension.microprofile.reactive.messaging.kafka.api.WildFlyKafkaMessage;
import org.wildfly.extension.microprofile.reactive.messaging.kafka.api.WildFlyKafkaMessageUtil;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class MessagingBean {

    @Inject
    WildFlyKafkaMessageUtil util;

    @Inject
    @Named("test-rebalance")
    RebalanceListener rebalanceListener;

    private MockExternalAsyncResource generator;

    @Outgoing("to-kafka")
    public CompletionStage<Message<String>> producer() {
        return generator.getNextValue("a");
    }

    @Incoming("from-kafka")
    public CompletionStage<Void> consumer(Message<String> msg) {
        System.out.println("+------  Received!");
        System.out.println(msg);

        WildFlyKafkaMessage<String, String> wfMsg = util.from(msg);
        System.out.println("Payload: " + wfMsg.getPayload());
        System.out.println("Headers: " + wfMsg.getHeaders());
        System.out.println("Partition: " + wfMsg.getPartition());
        System.out.println("Key: " + wfMsg.getKey());
        System.out.println("Topic: " + wfMsg.getTopic());
        System.out.println("Timestamp: " + wfMsg.getTimestamp());

        CompletableFuture<Void> cf = new CompletableFuture<>();
        cf.complete(null);
        return cf;
    }

    private class MockExternalAsyncResource {
        private static final int TICK = 2000;

        private ScheduledExecutorService delayedExecutor = Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory());
        private final AtomicInteger count = new AtomicInteger(0);
        private long last = System.currentTimeMillis();

        @PreDestroy
        public void stop() {
            delayedExecutor.shutdown();
        }

        public CompletionStage<Message<String>> getNextValue(String value) {
            synchronized (this) {
                CompletableFuture<Message<String>> cf = new CompletableFuture<>();
                long now = System.currentTimeMillis();
                long next = TICK + last;
                long delay = next - now;
                last = next;
                delayedExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        cf.complete(Message.of(value));
                    }
                }, delay, TimeUnit.MILLISECONDS);
                return cf;
            }
        }

    }

}
