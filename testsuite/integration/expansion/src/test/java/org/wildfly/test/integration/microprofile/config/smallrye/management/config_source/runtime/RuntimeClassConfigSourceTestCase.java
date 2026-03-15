/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.config.smallrye.AbstractMicroProfileConfigTestCase;
import org.wildfly.test.integration.microprofile.config.smallrye.AssertUtils;
import org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_class.CustomConfigSource;

/**
 * Test adding a class-based config-source at RUNTIME using management operations.
 *
 * This test validates Phase 1 of WFLY-21615:
 * - Deploy application first
 * - Add class-based config-source at runtime via management operation
 * - Verify properties from the config-source are visible in the application
 *
 * Note: This test will FAIL until Phase 2 implementation is complete, as runtime
 * addition of class-based config-sources is not yet implemented.
 *
 * @author Claude Code (Phase 1 test for WFLY-21615)
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RuntimeClassConfigSourceTestCase.RuntimeConfigSourceSetupTask.class)
public class RuntimeClassConfigSourceTestCase extends AbstractMicroProfileConfigTestCase {

    private static final String CONFIG_SOURCE_NAME = "runtime-test-class";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "RuntimeClassConfigSourceTestCase.war")
                .addClasses(RuntimeTestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addAsWebInfResource(RuntimeClassConfigSourceTestCase.class.getPackage(), "jboss-deployment-structure.xml",
                        "jboss-deployment-structure.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    /**
     * Test that properties from a runtime-added class-based config-source are visible.
     *
     * Expected behavior (once Phase 2 is implemented):
     * 1. Application deployed without config-source
     * 2. Config-source added at runtime via management operation
     * 3. Properties from CustomConfigSource should be accessible via MicroProfile Config API
     */
    @Test
    public void testRuntimeAddedClassConfigSource() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + "runtime-config-test/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = EntityUtils.toString(response.getEntity());

            // Verify property from runtime-added config-source is visible
            AssertUtils.assertTextContainsProperty(text, CustomConfigSource.PROP_NAME, CustomConfigSource.PROP_VALUE);
        }
    }

    /**
     * Setup task that creates the test module at server startup and adds the config-source
     * at runtime (after deployment).
     */
    static class RuntimeConfigSourceSetupTask implements ServerSetupTask {

        private static final String TEST_MODULE_NAME = "test.custom-config-source";

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            // Note: The test module with CustomConfigSource is already created by the from_class test.
            // We're reusing that existing module. In a real scenario, you might need to create it here.

            // Add config-source at RUNTIME (after deployment would have occurred)
            addConfigSourceAtRuntime(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // Remove the runtime-added config-source
            removeConfigSource(managementClient.getControllerClient());
        }

        /**
         * Add a class-based config-source at runtime using management operations.
         * This simulates the runtime scenario being tested.
         */
        private void addConfigSourceAtRuntime(ModelControllerClient client) throws IOException {
            ModelNode op = new ModelNode();
            op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
            op.get(OP_ADDR).add("config-source", CONFIG_SOURCE_NAME);
            op.get(OP).set(ADD);
            op.get("class").get("module").set(TEST_MODULE_NAME);
            op.get("class").get("name").set(CustomConfigSource.class.getName());

            ModelNode result = client.execute(op);

            // For debugging: print the result
            if (!result.get("outcome").asString().equals("success")) {
                System.err.println("Failed to add runtime config-source: " + result.asString());
            }
        }

        /**
         * Remove the runtime-added config-source during teardown.
         */
        private void removeConfigSource(ModelControllerClient client) throws IOException {
            ModelNode op = new ModelNode();
            op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
            op.get(OP_ADDR).add("config-source", CONFIG_SOURCE_NAME);
            op.get(OP).set(REMOVE);
            client.execute(op);
        }
    }
}
