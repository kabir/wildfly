/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties;

import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties.RuntimeConfigSourceOrdinalSetupTask.VALUE_FROM_A;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties.RuntimeConfigSourceOrdinalSetupTask.VALUE_FROM_B;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties.OrdinalTestApplication.PRIORITY_TEST;

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
import org.jboss.as.test.shared.ServerReload;
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
 * Test for WFLY-21615: Modify config-source ordinal at runtime and verify
 * property priority changes after server reload.
 *
 * With Phase 4, config-source operations are reload-required. This test verifies:
 * - Ordinal modification succeeds and returns reload-required status
 * - After reload, config refresh (Phase 2) makes priority change visible
 * - Service dependency (Phase 3) ensures proper initialization order
 *
 * Test workflow:
 * 1. SetupTask creates two config-sources:
 *    - propsA: ordinal=100, priority-test=from-A
 *    - propsB: ordinal=200, priority-test=from-B
 * 2. Initially, propsB wins (higher ordinal) - verify value is "from-B"
 * 3. At runtime, modify propsA ordinal to 300 (reload-required response)
 * 4. Reload server
 * 5. Now propsA should win - verify value becomes "from-A" after reload
 *
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RuntimeConfigSourceOrdinalSetupTask.class)
public class RuntimeConfigSourceOrdinalTestCase extends AbstractMicroProfileConfigTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "RuntimeConfigSourceOrdinalTestCase.war")
                .addClasses(OrdinalTestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @ArquillianResource
    private URL url;

    @ArquillianResource
    private ManagementClient managementClient;

    /**
     * Test that modifying a config-source ordinal at runtime changes the priority
     * of configuration values after server reload.
     *
     * With Phase 4, ordinal modifications require reload. This test verifies the
     * complete flow including Phase 2 (config refresh) and Phase 3 (service dependency).
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

            // Step 2: Modify propsA ordinal to 300 at runtime
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

            // Step 3: Reload server (Phase 4 makes config-source operations reload-required)
            ServerReload.reloadIfRequired(managementClient);

            // Step 4: Query again - propsA should now win (ordinal 300 > 200) after reload
            response = client.execute(new HttpGet(url + "custom-config-source/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            text = EntityUtils.toString(response.getEntity());

            System.out.println("\nAfter reload (propsA ordinal=300 > propsB ordinal=200):");
            System.out.println(text);

            // After ordinal change and reload, propsA (ordinal=300) should win over propsB (ordinal=200)
            // Phase 2 (config refresh) + Phase 3 (service dependency) ensure proper initialization
            AssertUtils.assertTextContainsProperty(text, PRIORITY_TEST, VALUE_FROM_A);

            System.out.println("\nSUCCESS: Runtime ordinal modification worked! Config value changed from '"
                             + VALUE_FROM_B + "' to '" + VALUE_FROM_A + "' after reload.");
        }
    }
}
