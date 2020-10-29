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

package org.wildfly.test.integration.microprofile.reactive.messaging.kafka;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@ApplicationScoped
public class Bean {
    private final CountDownLatch latch = new CountDownLatch(3);
    private StringBuilder phrase = new StringBuilder();

    @Inject
    ManagedExecutor executor;

    @Inject
    TransactionalBean txBean;

    public CountDownLatch getLatch() {
        return latch;
    }


    @Outgoing("to-kafka")
    public PublisherBuilder<String> source() {
        // We need to set the following in microprofile-config.properties for this approach to work
        //  mp.messaging.incoming.from-kafka.auto.offset.reset=earliest
        return ReactiveStreams.of("hello", "reactive", "messaging");
    }

    @Incoming("from-kafka")
    @Outgoing("sink")
    public CompletionStage<String> store(String payload) {
        // Use the executor to make sure it is on a separate thread
        CompletableFuture<String> ret = executor.completedFuture(payload);
        return ret.thenApplyAsync(v -> {
            if (v.equals("reactive")) {
                // Add a sleep here to make sure the calling method has returned
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e){
                    throw new RuntimeException(e);
                }
                txBean.storeValue(v);
            }
            return v;
        });
    }

    @Incoming("sink")
    public void sink(String word) {
        if (phrase.length() > 0) {
            phrase.append(" ");
        }
        this.phrase.append(word);
        latch.countDown();
    }

    public String getPhrase() {
        return phrase.toString();
    }
}
