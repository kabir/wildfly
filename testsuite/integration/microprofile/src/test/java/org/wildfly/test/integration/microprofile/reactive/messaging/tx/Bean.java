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

package org.wildfly.test.integration.microprofile.reactive.messaging.tx;

import java.util.Collections;
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

    @Outgoing("source")
    public PublisherBuilder<String> source() {
        txBean.checkValues(Collections.emptySet());
        return ReactiveStreams.of("hello", "reactive", "messaging");
    }

    @Incoming("source")
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
