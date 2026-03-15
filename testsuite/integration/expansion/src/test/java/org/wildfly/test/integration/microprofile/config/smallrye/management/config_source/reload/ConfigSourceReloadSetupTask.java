/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.reload;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.test.shared.ServerReload;
import org.jboss.dmr.ModelNode;

/**
 * Setup task for WFLY-21616 reload race condition test.
 *
 * Adds a config-source with properties, then triggers a server reload.
 * The reload tests whether the service dependency ensures config-sources
 * are fully initialized before dependent subsystems start.
 */
public class ConfigSourceReloadSetupTask implements ServerSetupTask {

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {
        addConfigSource(managementClient.getControllerClient());
        // Trigger reload to test service dependency
        ServerReload.reloadIfRequired(managementClient);
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        removeConfigSource(managementClient.getControllerClient());
        ServerReload.reloadIfRequired(managementClient);
    }

    private void addConfigSource(ModelControllerClient client) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
        op.get(OP_ADDR).add("config-source", "reload-test-config-source");
        op.get(OP).set(ADD);

        ModelNode properties = new ModelNode();
        properties.get("my.reload.test.property").set("reload-test-value");
        op.get("properties").set(properties);

        client.execute(op);
    }

    private void removeConfigSource(ModelControllerClient client) throws IOException {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
        op.get(OP_ADDR).add("config-source", "reload-test-config-source");
        op.get(OP).set(REMOVE);
        client.execute(op);
    }
}
