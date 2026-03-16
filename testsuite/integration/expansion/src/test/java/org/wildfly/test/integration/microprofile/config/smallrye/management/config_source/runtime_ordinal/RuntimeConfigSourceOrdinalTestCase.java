/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime_ordinal;

import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime_ordinal.SetupTask.VALUE_FROM_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime_ordinal.SetupTask.VALUE_FROM_B;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime_ordinal.TestApplication.PRIORITY_TEST;

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
import org.jboss.as.arquillian.container.ManagementClient;
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

/**
 * Test runtime modification of config-source ordinal values (WFLY-21615 Phase 1).
 *
 * This test verifies that modifying a config-source's ordinal at RUNTIME changes
 * the priority of config values without requiring a server reload.
 *
 * Test scenario:
 * 1. SetupTask creates two config-sources:
 *    - propsA: ordinal=100, priority-test=from-A
 *    - propsB: ordinal=200, priority-test=from-B
 * 2. Initially, propsB wins (higher ordinal) - verify value is "from-B"
 * 3. At runtime, modify propsA ordinal to 300 (higher than propsB)
 * 4. Now propsA should win - verify value becomes "from-A"
 *
 * EXPECTED BEHAVIOR (Phase 1):
 * This test will FAIL initially because the ordinal change requires server reload
 * to take effect. Phase 2 implementation will fix this by implementing runtime refresh.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SetupTask.class)
public class RuntimeConfigSourceOrdinalTestCase extends AbstractMicroProfileConfigTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "RuntimeConfigSourceOrdinalTestCase.war")
                .addClasses(TestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @ArquillianResource
    private URL url;

    @ArquillianResource
    private ManagementClient managementClient;

    /**
     * Test that modifying a config-source ordinal at runtime changes the priority
     * of configuration values without requiring a server reload.
     *
     * Phase 1: This test demonstrates the CURRENT behavior (ordinal changes require reload).
     * Phase 2: After implementation, this test should PASS (ordinal changes work at runtime).
     */
    @Test
    public void testRuntimeOrdinalModification() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            // Step 1: Verify initial state - propsB should win (ordinal 200 > 100)
            HttpResponse response = client.execute(new HttpGet(url + "custom-config-source/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = EntityUtils.toString(response.getEntity());

            System.out.println("Initial state (propsB ordinal=200 > propsA ordinal=100):");
            System.out.println(text);

            // Initially, propsB (ordinal=200) should win over propsA (ordinal=100)
            AssertUtils.assertTextContainsProperty(text, PRIORITY_TEST, VALUE_FROM_B);

            // Step 2: Modify propsA ordinal to 300 at RUNTIME (no reload)
            System.out.println("\nModifying propsA ordinal from 100 to 300...");
            ModelNode writeOrdinalOp = new ModelNode();
            writeOrdinalOp.get("address").add("subsystem", "microprofile-config-smallrye")
                         .add("config-source", "propsA");
            writeOrdinalOp.get("operation").set("write-attribute");
            writeOrdinalOp.get("name").set("ordinal");
            writeOrdinalOp.get("value").set(300);

            ModelNode result = managementClient.getControllerClient().execute(writeOrdinalOp);
            Assert.assertEquals("success", result.get("outcome").asString());

            // Verify the ordinal was actually changed in the management model
            ModelNode readOrdinalOp = new ModelNode();
            readOrdinalOp.get("address").add("subsystem", "microprofile-config-smallrye")
                        .add("config-source", "propsA");
            readOrdinalOp.get("operation").set("read-attribute");
            readOrdinalOp.get("name").set("ordinal");

            result = managementClient.getControllerClient().execute(readOrdinalOp);
            Assert.assertEquals("success", result.get("outcome").asString());
            int newOrdinal = result.get("result").asInt();
            Assert.assertEquals("Ordinal should be updated in management model", 300, newOrdinal);
            System.out.println("Management model updated: propsA ordinal = " + newOrdinal);

            // Step 3: Query again - propsA should now win (ordinal 300 > 200)
            response = client.execute(new HttpGet(url + "custom-config-source/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            text = EntityUtils.toString(response.getEntity());

            System.out.println("\nAfter runtime modification (propsA ordinal=300 > propsB ordinal=200):");
            System.out.println(text);

            // CRITICAL TEST: After ordinal change, propsA (ordinal=300) should win over propsB (ordinal=200)
            // Phase 1: This assertion will FAIL - ordinal change doesn't take effect without reload
            // Phase 2: This assertion should PASS - ordinal change takes effect immediately
            AssertUtils.assertTextContainsProperty(text, PRIORITY_TEST, VALUE_FROM_A);

            System.out.println("\nSUCCESS: Runtime ordinal modification worked! Config value changed from '"
                             + VALUE_FROM_B + "' to '" + VALUE_FROM_A + "' without reload.");
        }
    }

    /**
     * Additional test to verify that ordinal changes are reflected in the runtime
     * MicroProfile Config instance, not just the management model.
     *
     * This ensures the fix addresses the actual runtime behavior, not just model updates.
     */
    @Test
    public void testOrdinalChangeAffectsRuntimeConfig() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            // Initial query
            HttpResponse response = client.execute(new HttpGet(url + "custom-config-source/test"));
            String initialValue = EntityUtils.toString(response.getEntity());

            // Change ordinal
            ModelNode writeOrdinalOp = new ModelNode();
            writeOrdinalOp.get("address").add("subsystem", "microprofile-config-smallrye")
                         .add("config-source", "propsA");
            writeOrdinalOp.get("operation").set("write-attribute");
            writeOrdinalOp.get("name").set("ordinal");
            writeOrdinalOp.get("value").set(300);

            managementClient.getControllerClient().execute(writeOrdinalOp);

            // Query multiple times to ensure consistency
            for (int i = 0; i < 3; i++) {
                response = client.execute(new HttpGet(url + "custom-config-source/test"));
                String currentValue = EntityUtils.toString(response.getEntity());

                // All queries should return the same value (from-A after ordinal change)
                AssertUtils.assertTextContainsProperty(currentValue, PRIORITY_TEST, VALUE_FROM_A);

                // Small delay between queries
                Thread.sleep(100);
            }

            System.out.println("Runtime config consistently returns updated priority after ordinal change.");
        }
    }
}
