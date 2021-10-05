/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive.messaging.amqp;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.reactive.messaging.Outgoing;

@ApplicationScoped
public class ProducingBean {
    public static final int HIGH = 64;
    private static final int TICK = 100;
    private static final int TICK2 = 1000;

    private ScheduledExecutorService delayedExecutor = Executors.newSingleThreadScheduledExecutor(Executors.defaultThreadFactory());
    private volatile int value = 1;
    private long last = -1;

    @Outgoing("source")
    public CompletableFuture<Integer> generate() {
        System.out.println("----> Calling generate!!!!");
        synchronized (this) {
            CompletableFuture<Integer> cf = new CompletableFuture<>();
            if (last == -1) {
                last = System.currentTimeMillis();
            }
            int tick = value < HIGH ? TICK2 : TICK;
            long next = TICK + last;
            long delay = next - last;
            last = next;
            Next nextTask = new Next(cf, value);
            System.out.println("=====> Creating Next with " + value);
            if (value < HIGH) {
                value *= 2;
            }
            delayedExecutor.schedule(nextTask, delay , TimeUnit.MILLISECONDS);
            return cf;
        }
    }


    @PreDestroy
    public void stop() {
        delayedExecutor.shutdown();
    }

    private class Next implements Runnable {
        private final CompletableFuture<Integer> cf;
        private final int value;

        public Next(CompletableFuture<Integer> cf, int value) {
            this.cf = cf;
            this.value = value;
        }

        @Override
        public void run() {
            System.out.println("---> Sending " + value);
            cf.complete(value);
        }
    }

}
