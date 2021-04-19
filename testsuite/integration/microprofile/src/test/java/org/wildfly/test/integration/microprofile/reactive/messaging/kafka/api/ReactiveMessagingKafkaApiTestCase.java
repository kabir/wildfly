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

import java.io.StringReader;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.json.Json;
import javax.json.JsonArray;

import org.apache.kafka.common.TopicPartition;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.common.HttpRequest;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunKafkaSetupTask;

import kafka.server.KafkaServer;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup({RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class})
@RunAsClient
public class ReactiveMessagingKafkaApiTestCase {
    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive getDeployment() {

        // Set properties for the RunKafkaSetupTask
        //System.setProperty(RunKafkaSetupTask.NUM_PARTITIONS, "3");

        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-api.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingKafkaApiTestCase.class.getPackage())
                .addClasses(RunKafkaSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingKafkaApiTestCase.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties");

        return webArchive;
    }

    @Test
    public void testKafkaApi() throws Exception {
        JsonArray arr = readJsonArray();
        System.out.println(arr);

        //Thread.sleep(6000);
        EmbeddedKafkaBroker broker = RunKafkaSetupTask.getEmbeddedKafka();
        int a = broker.getPartitionsPerTopic();
        KafkaServer server = broker.getKafkaServer(0);
        server.replicaManager().createPartition(new TopicPartition("testing", 7));
        RunKafkaSetupTask.getEmbeddedKafka().restart(0);
        int b = broker.getPartitionsPerTopic();

        broker.brokerProperty("offsets.topic.num.partitions", 4);
        RunKafkaSetupTask.getEmbeddedKafka().restart(0);
        int c = broker.getPartitionsPerTopic();

        Thread.sleep(6000);
    }

    private JsonArray readJsonArray() throws Exception {
        URL url = new URL(this.url.toExternalForm());
        String s = HttpRequest.get(url.toExternalForm(),10, TimeUnit.SECONDS);
        JsonArray arr = Json.createReader(new StringReader(s)).readArray();
        return arr;
    }
}
