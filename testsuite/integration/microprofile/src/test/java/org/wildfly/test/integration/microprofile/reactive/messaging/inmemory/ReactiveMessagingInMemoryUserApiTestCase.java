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

package org.wildfly.test.integration.microprofile.reactive.messaging.inmemory;

import io.smallrye.reactive.messaging.kafka.api.IncomingKafkaRecordMetadata;
import jakarta.inject.Inject;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.record.TimestampType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PropertyPermission;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({ EnableReactiveExtensionsSetupTask.class})
public class ReactiveMessagingInMemoryUserApiTestCase {

    private static final long TIMEOUT = TimeoutUtil.adjust(15000);

    @Inject
    InDepthMetadataBean inDepthMetadataBean;

    @Deployment
    public static WebArchive getDeployment() {

        return ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-user-api.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingInMemoryUserApiTestCase.class.getPackage())
                .addClasses(EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingInMemoryUserApiTestCase.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
    }

    /*
     * This tests that:
     * - incoming Metadata is set (also for entry 6 which did not set any metadata) and contains the topic
     * - the key is propagated from what was set in the outgoing metadata, and that it may be null when not set
     * - Headers are propagated, if set in the outgoing metadata
     * - offsets are unique per partition
     * - the timestamp and type are set, and that the timestamp matches if we set it ourselves in the outgoing metadata
     */
    @Test
    public void testOutgoingAndIncomingMetadataExtensively() throws InterruptedException {
        inDepthMetadataBean.getLatch().await(TIMEOUT, TimeUnit.MILLISECONDS);
        Map<Integer, IncomingKafkaRecordMetadata<String, Integer>> map = inDepthMetadataBean.getMetadatas();

        Assert.assertEquals(6, map.size());
        Map<Integer, Set<Long>> offsetsByPartition = new HashMap<>();

        for (int i = 1; i <= 6; i++) {
            IncomingKafkaRecordMetadata metadata = map.get(i);
            Assert.assertNotNull(metadata);
            if (i != 6) {
                Assert.assertEquals("KEY-" + i, metadata.getKey());
            } else {
                Assert.assertNull(metadata.getKey());
            }
            Assert.assertEquals("testing1", metadata.getTopic());
            Set<Long> offsets = offsetsByPartition.get(metadata.getPartition());
            if (offsets == null) {
                offsets = new HashSet<>();
                offsetsByPartition.put(metadata.getPartition(), offsets);
            }
            offsets.add(metadata.getOffset());
            Assert.assertNotNull(metadata.getTimestamp());
            if (i == 5) {
                Assert.assertEquals(inDepthMetadataBean.getTimestampEntry5Topic1(), metadata.getTimestamp());
            }
            Assert.assertEquals(TimestampType.CREATE_TIME, metadata.getTimestampType());
            Assert.assertNotNull(metadata.getRecord());

            Headers headers = metadata.getHeaders();
            if (i != 5) {
                Assert.assertEquals(0, headers.toArray().length);
            } else {
                Assert.assertEquals(1, headers.toArray().length);
                Header header = headers.toArray()[0];
                Assert.assertEquals("simple", header.key());
                Assert.assertArrayEquals(new byte[]{0, 1, 2}, header.value());
            }
        }
        Assert.assertEquals(6, checkOffsetsByPartitionAndCalculateTotalEntries(offsetsByPartition));
    }

    private int checkOffsetsByPartitionAndCalculateTotalEntries(Map<Integer, Set<Long>> offsetsByPartition) {
        int total = 0;
        for (Iterator<Set<Long>> it = offsetsByPartition.values().iterator(); it.hasNext() ; ) {
            Set<Long> offsets = it.next();
            long size = offsets.size();
            total += size;
            for (long l = 0; l < size; l++) {
                Assert.assertTrue(offsets.contains(l));
            }
        }
        return total;
    }


}
