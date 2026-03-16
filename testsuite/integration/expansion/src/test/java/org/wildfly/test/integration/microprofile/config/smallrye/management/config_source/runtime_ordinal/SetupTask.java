/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime_ordinal;

import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime_ordinal.TestApplication.PRIORITY_TEST;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;

/**
 * Add two config-sources (propsA and propsB) with different ordinals to test runtime ordinal modification.
 * Both config-sources contain the same property name with different values.
 * Initial state: propsB has higher ordinal (200) than propsA (100), so propsB should win.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
public class SetupTask extends CLIServerSetupTask {
    static final String ADDR_A = "/subsystem=microprofile-config-smallrye/config-source=propsA";
    static final String ADDR_B = "/subsystem=microprofile-config-smallrye/config-source=propsB";

    static final String VALUE_FROM_A = "from-A";
    static final String VALUE_FROM_B = "from-B";

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        NodeBuilder nb = builder.node(containerId);

        // propsA: ordinal=100, priority-test=from-A
        String propsA = String.format("{%s=%s}", PRIORITY_TEST, VALUE_FROM_A);

        // propsB: ordinal=200, priority-test=from-B
        String propsB = String.format("{%s=%s}", PRIORITY_TEST, VALUE_FROM_B);

        // Add propsA with ordinal=100
        nb.setup(String.format("%s:add(properties=%s, ordinal=100)", ADDR_A, propsA));

        // Add propsB with ordinal=200 (higher priority initially)
        nb.setup(String.format("%s:add(properties=%s, ordinal=200)", ADDR_B, propsB));

        nb.teardown(String.format("%s:remove", ADDR_A));
        nb.teardown(String.format("%s:remove", ADDR_B));

        super.setup(managementClient, containerId);
    }
}
