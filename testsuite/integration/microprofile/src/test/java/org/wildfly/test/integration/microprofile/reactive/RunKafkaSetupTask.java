/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SYSTEM_PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;

import io.smallrye.reactive.messaging.kafka.companion.KafkaCompanion;
import org.jboss.arquillian.testcontainers.api.DockerRequired;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.model.test.ModelTestUtils;
import org.jboss.dmr.ModelNode;
import org.testcontainers.kafka.KafkaContainer;
import org.wildfly.security.manager.WildFlySecurityManager;

@DockerRequired
public class RunKafkaSetupTask implements ServerSetupTask {
    volatile KafkaContainer container;
    volatile KafkaCompanion companion;

    private static final int MAIN_KAFKA_PORT = 9092;

    private static final PathElement KAFKA_PORT_PATH = PathElement.pathElement(SYSTEM_PROPERTY, "calculated.kafka.port");

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        String kafkaVersion = WildFlySecurityManager.getPropertyPrivileged("wildfly.test.kafka.version", null);
        if (kafkaVersion == null) {
            throw new IllegalArgumentException("Specify Kafka version with -Dwildfly.test.kafka.version");
        }
        container = new KafkaContainer("apache/kafka-native:" + kafkaVersion);
        container.addExposedPort(MAIN_KAFKA_PORT);

        for (Map.Entry<String, String> entry : extraBrokerProperties().entrySet()) {
            container.addEnv(entry.getKey(), entry.getValue());
        }

        container.start();
        int port = container.getMappedPort(MAIN_KAFKA_PORT);

        companion = new KafkaCompanion("INTERNAL://localhost:" + port);

        Map<String, Integer> topicsAndPartitions = getTopicsAndPartitions();
        if (topicsAndPartitions == null || topicsAndPartitions.isEmpty()) {
            companion.topics().createAndWait("testing", 1, Duration.of(10, ChronoUnit.SECONDS));
        } else {
            for (Map.Entry<String, Integer> entry : topicsAndPartitions.entrySet()) {
                companion.topics().createAndWait(entry.getKey(), entry.getValue(), Duration.of(10, ChronoUnit.SECONDS));
            }
        }

        setKafkaPortInModel(managementClient, port);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        removeKafkaPortInModel(managementClient);

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

    public static void setKafkaPortInModel(ManagementClient managementClient, int port) throws IOException {
            // Set the calculated port as a property in the model
            ModelNode op = Util.createAddOperation(PathAddress.pathAddress(KAFKA_PORT_PATH), Map.of(VALUE, new ModelNode(port)));
            ModelNode result =  managementClient.getControllerClient().execute(op);
            ModelTestUtils.checkOutcome(result);
    }

    public static void removeKafkaPortInModel(ManagementClient managementClient) throws IOException {
        ModelNode op = Util.createRemoveOperation(PathAddress.pathAddress(KAFKA_PORT_PATH));
        managementClient.getControllerClient().execute(op);
    }

    protected Map<String, String> extraBrokerProperties() {
        return Collections.emptyMap();
    }

    protected Map<String, Integer> getTopicsAndPartitions() {
        return null;
    }

}
