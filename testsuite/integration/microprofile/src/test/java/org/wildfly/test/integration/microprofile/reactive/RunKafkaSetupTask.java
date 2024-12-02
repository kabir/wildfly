/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_RESOURCE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.wildfly.security.manager.WildFlySecurityManager;

@DockerRequired
public class RunKafkaSetupTask implements ServerSetupTask {
    private volatile KafkaContainer container;
    private volatile KafkaCompanion companion;

    private static final String KAFKA_PORT = "org.wildfly.test.kafka.port";
    private static final PathAddress KAFKA_PORT_PROPERTY_ADDR = PathAddress.pathAddress(SYSTEM_PROPERTY, KAFKA_PORT);
//    private static final PathAddress BOOTSTRAP_SERVERS_PROPERTY_ADDR = PathAddress.pathAddress(SYSTEM_PROPERTY, "kafka.bootstrap.servers");

    private static final PathAddress MP_CONFIG_SOURCE_ADDR =
            PathAddress.pathAddress(SUBSYSTEM, "microprofile-config-smallrye")
                    .append("config-source", "testing-kafka");


    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        String kafkaVersion = WildFlySecurityManager.getPropertyPrivileged("wildfly.test.kafka.version", null);
        if (kafkaVersion == null) {
            throw new IllegalArgumentException("Specify Kafka version with -Dwildfly.test.kafka.version");
        }
        container = new KafkaContainer("apache/kafka-native:" + kafkaVersion);
        container.setExposedPorts(List.of(9092, 9093));

        for (Map.Entry<String, String> entry : extraBrokerProperties().entrySet()) {
            container.addEnv(entry.getKey(), entry.getValue());
        }

        container.start();

        companion = new KafkaCompanion("INTERNAL://localhost:" + container.getMappedPort(9092));

        Map<String, Integer> topicsAndPartitions = getTopicsAndPartitions();
        if (topicsAndPartitions == null || topicsAndPartitions.isEmpty()) {
            companion.topics().createAndWait("testing", 1, Duration.of(10, ChronoUnit.SECONDS));
        } else {
            for (Map.Entry<String, Integer> entry : topicsAndPartitions.entrySet()) {
                companion.topics().createAndWait(entry.getKey(), entry.getValue(), Duration.of(10, ChronoUnit.SECONDS));
            }
        }
        addKafkaPortPropertyToModel(managementClient, container, 9092);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        removeKafkaPortPropertyFromModel(managementClient);
        ModelNode result = managementClient.getControllerClient().execute(Util.createOperation(READ_RESOURCE_OPERATION, KAFKA_PORT_PROPERTY_ADDR));
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            managementClient.getControllerClient().execute(Util.createRemoveOperation(KAFKA_PORT_PROPERTY_ADDR));
        }
        if (companion != null) {
            try {
                companion.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (container != null) {
            try {
                container.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void addKafkaPortPropertyToModel(ManagementClient client, GenericContainer<?> container, int mappedPort) throws IOException {
        int mapped = container.getMappedPort(mappedPort);
        ModelNode result = client.getControllerClient().execute(
                Util.createAddOperation(KAFKA_PORT_PROPERTY_ADDR, Map.of(VALUE, new ModelNode(mapped))));
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

//        result = client.getControllerClient().execute(
//                Util.createAddOperation(BOOTSTRAP_SERVERS_PROPERTY_ADDR, Map.of(VALUE, new ModelNode("localhost:" + mapped + ""))));
//        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
//
//        ModelNode op = Util.createAddOperation(MP_CONFIG_SOURCE_ADDR);
//        op.get(PROPERTIES).add(KAFKA_PORT, new ModelNode(mapped));
//        result = client.getControllerClient().execute(op);
//        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

    }

    public static void removeKafkaPortPropertyFromModel(ManagementClient client) throws IOException {
        ModelNode result = client.getControllerClient().execute(Util.createOperation(READ_RESOURCE_OPERATION, KAFKA_PORT_PROPERTY_ADDR));
        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
            client.getControllerClient().execute(Util.createRemoveOperation(KAFKA_PORT_PROPERTY_ADDR));
        }

//        result = client.getControllerClient().execute(Util.createOperation(READ_RESOURCE_OPERATION, BOOTSTRAP_SERVERS_PROPERTY_ADDR));
//        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
//            client.getControllerClient().execute(Util.createRemoveOperation(BOOTSTRAP_SERVERS_PROPERTY_ADDR));
//        }
//
//        result = client.getControllerClient().execute(Util.createOperation(READ_RESOURCE_OPERATION, MP_CONFIG_SOURCE_ADDR));
//        if (SUCCESS.equals(result.get(OUTCOME).asString())) {
//            client.getControllerClient().execute(Util.createRemoveOperation(MP_CONFIG_SOURCE_ADDR));
//        }
    }

    protected Map<String, String> extraBrokerProperties() {
        return Collections.emptyMap();
    }

    protected Map<String, Integer> getTopicsAndPartitions() {
        return null;
    }

}
