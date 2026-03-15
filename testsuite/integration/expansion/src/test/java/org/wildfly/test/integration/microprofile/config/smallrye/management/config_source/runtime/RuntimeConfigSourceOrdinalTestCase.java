/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime;

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
 * Test for WFLY-21615: Change config-source ordinal at RUNTIME and verify priority changes.
 *
 * This test is expected to FAIL initially because config instances don't see ordinal changes.
 * Once WFLY-21615 is fixed (Phase 2), this test should pass.
 *
 * Test workflow:
 * 1. Deploy the application
 * 2. Add config-source A with ordinal=100 and property=value-A
 * 3. Add config-source B with ordinal=200 and property=value-B
 * 4. Query property - should get value-B (higher ordinal wins)
 * 5. Change config-source B ordinal to 50 at RUNTIME
 * 6. Query property - should get value-A (will FAIL until Phase 2 fixes it)
 * 7. Clean up
 *
 * @author Claude (AI Assistant)
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RuntimeConfigSourceOrdinalTestCase extends AbstractMicroProfileConfigTestCase {

    private static final String CONFIG_SOURCE_A = "runtime-ordinal-a";
    private static final String CONFIG_SOURCE_B = "runtime-ordinal-b";
    private static final String PROPERTY_NAME = "ordinal.test.property";
    private static final String VALUE_A = "value-from-source-a";
    private static final String VALUE_B = "value-from-source-b";

    @ArquillianResource
    private ManagementClient managementClient;

    @ArquillianResource
    private URL url;

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "RuntimeConfigSourceOrdinalTestCase.war")
                .addClasses(RuntimeQueryTestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testRuntimeOrdinalChange() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {

            // Step 1: Add config-source A with ordinal=100
            addConfigSource(CONFIG_SOURCE_A, VALUE_A, 100);

            // Step 2: Add config-source B with ordinal=200
            addConfigSource(CONFIG_SOURCE_B, VALUE_B, 200);

            // Step 3: Query property - should get value-B (higher ordinal)
            HttpResponse initialResponse = client.execute(new HttpGet(url + "runtime-query-test/test?property=" + PROPERTY_NAME));
            Assert.assertEquals(200, initialResponse.getStatusLine().getStatusCode());
            String initialText = EntityUtils.toString(initialResponse.getEntity());

            Assert.assertTrue("Property should have value from source B (ordinal 200). Response: " + initialText,
                    initialText.contains(PROPERTY_NAME + " = " + VALUE_B));

            // Step 4: Change config-source B ordinal to 50 (lower than A)
            changeOrdinal(CONFIG_SOURCE_B, 50);

            // Step 5: Query again - should NOW get value-A (source A ordinal 100 > source B ordinal 50)
            // THIS WILL FAIL with WFLY-21615 bug because Config instances don't refresh
            HttpResponse updatedResponse = client.execute(new HttpGet(url + "runtime-query-test/test?property=" + PROPERTY_NAME));
            Assert.assertEquals(200, updatedResponse.getStatusLine().getStatusCode());
            String updatedText = EntityUtils.toString(updatedResponse.getEntity());

            // This assertion will FAIL until WFLY-21615 is fixed
            Assert.assertTrue(
                    "After lowering source B ordinal to 50, property should come from source A (ordinal 100). " +
                    "Expected: '" + PROPERTY_NAME + " = " + VALUE_A + "' " +
                    "Actual response: " + updatedText + ". " +
                    "THIS IS EXPECTED TO FAIL - demonstrates WFLY-21615 bug.",
                    updatedText.contains(PROPERTY_NAME + " = " + VALUE_A));
        }
    }

    @After
    public void cleanup() throws Exception {
        removeConfigSource(CONFIG_SOURCE_A);
        removeConfigSource(CONFIG_SOURCE_B);
    }

    private void addConfigSource(String name, String value, int ordinal) throws Exception {
        PathAddress configSourceAddress = PathAddress.pathAddress("subsystem", "microprofile-config-smallrye")
                .append("config-source", name);

        String properties = String.format("{%s=%s}", PROPERTY_NAME, value);

        ModelNode addOperation = Util.createAddOperation(configSourceAddress);
        addOperation.get("properties").set(properties);
        addOperation.get("ordinal").set(ordinal);

        ModelNode result = managementClient.getControllerClient().execute(addOperation);
        Assert.assertEquals("Adding config-source " + name + " should succeed: " + result.toString(),
                "success", result.get("outcome").asString());
    }

    private void changeOrdinal(String name, int newOrdinal) throws Exception {
        PathAddress configSourceAddress = PathAddress.pathAddress("subsystem", "microprofile-config-smallrye")
                .append("config-source", name);

        ModelNode writeOperation = Util.getWriteAttributeOperation(configSourceAddress, "ordinal", newOrdinal);

        ModelNode result = managementClient.getControllerClient().execute(writeOperation);
        Assert.assertEquals("Changing ordinal for " + name + " should succeed: " + result.toString(),
                "success", result.get("outcome").asString());
    }

    private void removeConfigSource(String name) throws Exception {
        PathAddress configSourceAddress = PathAddress.pathAddress("subsystem", "microprofile-config-smallrye")
                .append("config-source", name);

        ModelNode removeOperation = Util.createRemoveOperation(configSourceAddress);

        try {
            ModelNode result = managementClient.getControllerClient().execute(removeOperation);
            if (!"success".equals(result.get("outcome").asString())) {
                System.out.println("Warning: Failed to remove config-source " + name + ": " + result.toString());
            }
        } catch (Exception e) {
            System.out.println("Warning: Exception during config-source " + name + " removal: " + e.getMessage());
        }
    }
}
