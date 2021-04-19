/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive;

import java.util.List;

import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class EnableReactiveExtensionsSetupTask extends CLIServerSetupTask {
    private static final String MODULE_REACTIVE_MESSAGING = "org.wildfly.extension.microprofile.reactive-messaging-smallrye";
    private static final String MODULE_REACTIVE_STREAMS_OPERATORS = "org.wildfly.extension.microprofile.reactive-streams-operators-smallrye";
    private static final String MODULE_CONTEXT_PROPAGATION = "org.wildfly.extension.microprofile.context-propagation-smallrye";
    private static final String SUBSYSTEM_REACTIVE_MESSAGING = "microprofile-reactive-messaging-smallrye";
    private static final String SUBSYSTEM_REACTIVE_STREAMS_OPERATORS = "microprofile-reactive-streams-operators-smallrye";
    private static final String SUBSYSTEM_CONTEXT_PROPAGATION = "microprofile-context-propagation-smallrye";
    public static final String EXTENSION_ADD = "/extension=%s:add";
    public static final String EXTENSION_REMOVE = "/extension=%s:remove";
    public static final String SUBSYSTEM_ADD = "/subsystem=%s:add";
    public static final String SUBSYSTEM_REMOVE = "/subsystem=%s:remove";


    public EnableReactiveExtensionsSetupTask() {
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        boolean rsoExt = !containsChild(managementClient, "extension", MODULE_REACTIVE_STREAMS_OPERATORS);
        boolean rsoSs = !containsChild(managementClient, "subsystem", SUBSYSTEM_REACTIVE_STREAMS_OPERATORS);
        boolean rmExt = !containsChild(managementClient, "extension", MODULE_REACTIVE_MESSAGING);
        boolean rmSs = !containsChild(managementClient, "subsystem", SUBSYSTEM_REACTIVE_MESSAGING);
        boolean cpExt = !containsChild(managementClient, "extension", MODULE_CONTEXT_PROPAGATION);
        boolean cpSs = !containsChild(managementClient, "subsystem", SUBSYSTEM_CONTEXT_PROPAGATION);

        NodeBuilder nb = this.builder.node(containerId);
        if (rsoExt) {
            nb.setup(EXTENSION_ADD, MODULE_REACTIVE_STREAMS_OPERATORS);
        }
        if (cpExt) {
            nb.setup(EXTENSION_ADD, MODULE_CONTEXT_PROPAGATION);
        }
        if (rmExt) {
            nb.setup(EXTENSION_ADD, MODULE_REACTIVE_MESSAGING);
        }
        if (rsoSs) {
            nb.setup(SUBSYSTEM_ADD, SUBSYSTEM_REACTIVE_STREAMS_OPERATORS);
        }
        if (cpSs) {
            nb.setup(SUBSYSTEM_ADD, SUBSYSTEM_CONTEXT_PROPAGATION);
        }
        if (rmSs) {
            nb.setup(SUBSYSTEM_ADD, SUBSYSTEM_REACTIVE_MESSAGING);
        }
        if (rmSs) {
            nb.teardown(SUBSYSTEM_REMOVE, SUBSYSTEM_REACTIVE_MESSAGING);
        }
        if (cpSs) {
            nb.teardown(SUBSYSTEM_REMOVE, SUBSYSTEM_CONTEXT_PROPAGATION);
        }
        if (rsoSs) {
            nb.teardown(SUBSYSTEM_REMOVE, SUBSYSTEM_REACTIVE_STREAMS_OPERATORS);
        }
        if (rmExt) {
            nb.teardown(EXTENSION_REMOVE, MODULE_REACTIVE_MESSAGING);
        }
        if (cpSs) {
            nb.teardown(EXTENSION_REMOVE, MODULE_CONTEXT_PROPAGATION);
        }
        if (rsoSs) {
            nb.teardown(EXTENSION_REMOVE, MODULE_REACTIVE_STREAMS_OPERATORS);
        }
        super.setup(managementClient, containerId);
    }

    private boolean containsChild(ManagementClient managementClient, String childType, String childName) throws Exception {
        ModelNode op = new ModelNode();
        op.get("operation").set("read-children-names");
        op.get("child-type").set(childType);
        ModelNode result = managementClient.getControllerClient().execute(op);
        if (!result.get("outcome").asString().equals("success")) {
            throw new IllegalStateException(result.asString());
        }
        List<ModelNode> names = result.get("result").asList();
        for (ModelNode name : names) {
            if (name.asString().equals(childName)) {
                return true;
            }
        }
        return false;
    }
}
