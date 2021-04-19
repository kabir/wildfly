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

package org.wildfly.extension.microprofile.reactive.messaging.kafka.bridge;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.kafka.common.header.Headers;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Metadata;
import org.wildfly.extension.microprofile.reactive.messaging.kafka.api.WildFlyKafkaMessage;

import io.smallrye.reactive.messaging.kafka.KafkaRecord;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WildFlyKafkaMessageImpl<K, T> implements WildFlyKafkaMessage<K, T> {
    private final KafkaRecord<K, T> record;

    public WildFlyKafkaMessageImpl(KafkaRecord<K, T> record) {
        this.record = record;
    }

    public K getKey() {
        return record.getKey();
    }

    public String getTopic() {
        return record.getTopic();
    }

    public int getPartition() {
        return record.getPartition();
    }

    public Instant getTimestamp() {
        return record.getTimestamp();
    }

    public Headers getHeaders() {
        return record.getHeaders();
    }

    @Override
    public T getPayload() {
        return record.getPayload();
    }

    @Override
    public Supplier<CompletionStage<Void>> getAck() {
        return record.getAck();
    }

    @Override
    public Function<Throwable, CompletionStage<Void>> getNack() {
        return record.getNack();
    }

    @Override
    public CompletionStage<Void> ack() {
        return record.ack();
    }

    @Override
    public CompletionStage<Void> nack(Throwable reason) {
        return record.nack(reason);
    }

    @Override
    public <C> C unwrap(Class<C> unwrapType) {
        return record.unwrap(unwrapType);
    }

    // Not in spec yet, just used behind the scenes in the SmallRye API
    @Override
    public Metadata getMetadata() {
        return record.getMetadata();
    }

    // Not in spec yet, just used behind the scenes in the SmallRye API
    @Override
    public <M> Optional<M> getMetadata(Class<? extends M> clazz) {
        return record.getMetadata(clazz);
    }

    // Not in spec yet, just used behind the scenes in the SmallRye API
    @Override
    public Message<T> addMetadata(Object metadata) {
        return record.addMetadata(metadata);
    }
}
