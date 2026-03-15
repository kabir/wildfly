/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * Add a config-source with properties for runtime removal testing.
 *
 * @author WildFly Team
 */
public class SetupTask extends CLIServerSetupTask {
    private static final String CONFIG_SOURCE_NAME = "runtime-test-source";
    private static final String ADDR = "/subsystem=microprofile-config-smallrye/config-source=" + CONFIG_SOURCE_NAME;

    static final String TEST_PROPERTY_NAME = "runtime.test.property";
    static final String TEST_PROPERTY_VALUE = "initial-value";

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        NodeBuilder nb = builder.node(containerId);

        String properties = String.format("{%s=%s}", TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

        // Add config-source with the test property
        nb.setup(String.format("%s:add(properties=%s)", ADDR, properties));

        // Teardown: remove config-source (backup cleanup in case test fails)
        nb.teardown(String.format("%s:remove", ADDR));

        super.setup(managementClient, containerId);
    }
}
