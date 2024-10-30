/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.shared.observability.setuptasks;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

public class OpenTelemetrySetupTask extends AbstractSetupTask {
    protected static final String SUBSYSTEM_NAME = "opentelemetry";
    protected static final ModelNode extensionAddress = Operations.createAddress("extension", "org.wildfly.extension.opentelemetry");
    protected static final ModelNode subsystemAddress = Operations.createAddress("subsystem", SUBSYSTEM_NAME);

    @Override
    public void setup(final ManagementClient managementClient, final String containerId) throws Exception {
        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, extensionAddress))) {
            executeOp(managementClient, Operations.createAddOperation(extensionAddress));
        }

        if (!Operations.isSuccessfulOutcome(executeRead(managementClient, subsystemAddress))) {
            executeOp(managementClient, Operations.createAddOperation(subsystemAddress));
        }

        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "batch-delay", "1"));
        executeOp(managementClient, writeAttribute(SUBSYSTEM_NAME, "sampler-type", "on"));

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }

    @Override
    public void tearDown(final ManagementClient managementClient, final String containerId) throws Exception {
        executeOp(managementClient, Operations.createRemoveOperation(subsystemAddress));
        executeOp(managementClient, Operations.createRemoveOperation(extensionAddress));

        ServerReload.executeReloadAndWaitForCompletion(managementClient);
    }
}
