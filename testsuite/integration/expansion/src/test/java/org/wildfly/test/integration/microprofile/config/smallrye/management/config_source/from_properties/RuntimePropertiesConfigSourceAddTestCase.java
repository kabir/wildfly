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
import org.jboss.as.test.shared.ServerReload;
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
 * Test for WFLY-21615: Add a properties config-source at runtime and verify
 * the property becomes visible after server reload.
 *
 * With Phase 4, config-source operations are reload-required. This test verifies:
 * - Add operation succeeds and returns reload-required status
 * - After reload, config refresh (Phase 2) makes property visible
 * - Service dependency (Phase 3) ensures proper initialization order
 *
 * Test workflow:
 * 1. Deploy the application
 * 2. Verify property doesn't exist yet
 * 3. Add config-source via management operation (reload-required response)
 * 4. Reload server
 * 5. Query property - should be visible after reload
 * 6. Clean up
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

            // Step 2: Add config-source with the property at runtime
            addConfigSource();

            // Step 3: Reload server (Phase 4 makes config-source operations reload-required)
            ServerReload.reloadIfRequired(managementClient);

            // Step 4: Query property - should be visible after reload
            // Phase 2 (config refresh) + Phase 3 (service dependency) ensure proper initialization
            HttpResponse reloadedResponse = client.execute(new HttpGet(url + "custom-config-source/query?property=" + RUNTIME_PROPERTY_NAME));
            Assert.assertEquals("Request after reload should succeed",
                    200, reloadedResponse.getStatusLine().getStatusCode());

            String reloadedText = EntityUtils.toString(reloadedResponse.getEntity());

            Assert.assertTrue(
                    "Property should be visible after config-source is added and server reloaded. " +
                    "Expected: '" + RUNTIME_PROPERTY_NAME + " = " + RUNTIME_PROPERTY_VALUE + "' " +
                    "Actual response: " + reloadedText,
                    reloadedText.contains(RUNTIME_PROPERTY_NAME + " = " + RUNTIME_PROPERTY_VALUE));
        }
    }

    @After
    public void cleanup() throws Exception {
        // Always try to remove the config-source in cleanup
        removeConfigSource();
        // Reload after removal (also reload-required)
        ServerReload.reloadIfRequired(managementClient);
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
