/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties;

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
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.config.smallrye.AbstractMicroProfileConfigTestCase;

/**
 * Test for WFLY-21615: Add a properties config-source at RUNTIME (not during setup) and verify
 * the property becomes visible without server reload.
 *
 * This test is expected to FAIL initially because config instances don't see runtime changes.
 * Once WFLY-21615 is fixed (Phase 2), this test should pass.
 *
 * Test workflow:
 * 1. Deploy the application FIRST
 * 2. Query for a property that doesn't exist (should get 404 or empty response)
 * 3. Add config-source via management operation at RUNTIME (not in SetupTask)
 * 4. Query again - property should be visible (will FAIL until Phase 2 fixes it)
 * 5. Clean up - remove the config-source
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RuntimePropertiesConfigSourceAddTestCase extends AbstractMicroProfileConfigTestCase {

    private static final String CONFIG_SOURCE_NAME = "runtime-test-props";
    private static final String RUNTIME_PROPERTY_NAME = "runtime.test.property";
    private static final String RUNTIME_PROPERTY_VALUE = "runtime-value";

    @ArquillianResource
    private ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "RuntimePropertiesConfigSourceAddTestCase.war")
                .addClasses(RuntimeTestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testRuntimeConfigSourceAdd() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            // Step 1: Verify property does NOT exist before adding config-source
            HttpResponse initialResponse = client.execute(new HttpGet(url + "custom-config-source/query?property=" + RUNTIME_PROPERTY_NAME));
            String initialText = EntityUtils.toString(initialResponse.getEntity());

            // Property should not be found initially (404 or empty/null value)
            Assert.assertTrue("Property should not exist before config-source is added. Response: " + initialText,
                    initialResponse.getStatusLine().getStatusCode() == 404 ||
                    initialText.contains("null") ||
                    initialText.contains("not found") ||
                    initialText.isEmpty());

            // Step 2: Add config-source with the property at RUNTIME
            addConfigSource();

            // Step 3: Query again - property SHOULD be visible now
            // THIS WILL FAIL with WFLY-21615 bug because Config instances don't refresh
            HttpResponse runtimeResponse = client.execute(new HttpGet(url + "custom-config-source/query?property=" + RUNTIME_PROPERTY_NAME));
            Assert.assertEquals("Request after adding config-source should succeed",
                    200, runtimeResponse.getStatusLine().getStatusCode());

            String runtimeText = EntityUtils.toString(runtimeResponse.getEntity());

            // This assertion will FAIL until WFLY-21615 is fixed (Phase 2 implementation)
            Assert.assertTrue(
                    "Property should be visible after config-source is added at runtime. " +
                    "Expected: '" + RUNTIME_PROPERTY_NAME + " = " + RUNTIME_PROPERTY_VALUE + "' " +
                    "Actual response: " + runtimeText + ". " +
                    "THIS IS EXPECTED TO FAIL - demonstrates WFLY-21615 bug where config instances don't see runtime changes.",
                    runtimeText.contains(RUNTIME_PROPERTY_NAME + " = " + RUNTIME_PROPERTY_VALUE));
        }
    }

    @After
    public void cleanup() throws Exception {
        // Always try to remove the config-source in cleanup
        removeConfigSource();
    }

    /**
     * Add a config-source with properties via management operation at RUNTIME.
     */
    private void addConfigSource() throws Exception {
        PathAddress configSourceAddress = PathAddress.pathAddress("subsystem", "microprofile-config-smallrye")
                .append("config-source", CONFIG_SOURCE_NAME);

        // Create the properties ModelNode object
        ModelNode properties = new ModelNode();
        properties.get(RUNTIME_PROPERTY_NAME).set(RUNTIME_PROPERTY_VALUE);

        ModelNode addOperation = Util.createAddOperation(configSourceAddress);
        addOperation.get("properties").set(properties);

        ModelNode result = managementClient.getControllerClient().execute(addOperation);
        Assert.assertEquals("Adding config-source should succeed: " + result.toString(),
                "success", result.get("outcome").asString());
    }

    /**
     * Remove the config-source via management operation.
     */
    private void removeConfigSource() throws Exception {
        PathAddress configSourceAddress = PathAddress.pathAddress("subsystem", "microprofile-config-smallrye")
                .append("config-source", CONFIG_SOURCE_NAME);

        ModelNode removeOperation = Util.createRemoveOperation(configSourceAddress);

        try {
            ModelNode result = managementClient.getControllerClient().execute(removeOperation);
            // Don't fail if removal fails (config-source might not exist)
            if (!"success".equals(result.get("outcome").asString())) {
                System.out.println("Warning: Failed to remove config-source (may not exist): " + result.toString());
            }
        } catch (Exception e) {
            // Log but don't fail cleanup
            System.out.println("Warning: Exception during config-source removal: " + e.getMessage());
        }
    }
}
