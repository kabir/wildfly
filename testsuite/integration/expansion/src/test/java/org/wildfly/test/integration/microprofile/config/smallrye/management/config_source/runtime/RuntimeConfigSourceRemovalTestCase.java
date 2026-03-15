/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime;

import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime.SetupTask.TEST_PROPERTY_NAME;
import static org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime.SetupTask.TEST_PROPERTY_VALUE;

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

/**
 * Test runtime removal of a config-source from the microprofile-config subsystem.
 *
 * This test validates WFLY-21615 Phase 1 behavior:
 * - A config-source is added during setup with a known property
 * - The deployed application successfully reads the property
 * - The config-source is removed at RUNTIME (no reload)
 * - The application is queried again
 *
 * EXPECTED BEHAVIOR IN PHASE 1: This test will FAIL because existing Config instances
 * do not automatically see the runtime change. The property will still be returned even
 * though the config-source was removed.
 *
 * EXPECTED BEHAVIOR IN PHASE 2: After implementing config instance refresh on runtime
 * changes, this test should PASS - the property should not be found after removal.
 *
 * @author WildFly Team
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(SetupTask.class)
public class RuntimeConfigSourceRemovalTestCase extends AbstractMicroProfileConfigTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "RuntimeConfigSourceRemovalTestCase.war")
                .addClasses(TestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @ArquillianResource
    private URL url;

    @ArquillianResource
    private ModelControllerClient controllerClient;

    /**
     * Test that removing a config-source at RUNTIME makes its properties disappear.
     *
     * This test will FAIL in Phase 1 because Config instances are not refreshed on runtime changes.
     */
    @Test
    public void testRuntimeConfigSourceRemoval() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            // Step 1: Verify property exists (added by SetupTask)
            HttpResponse response = client.execute(new HttpGet(url + "runtime-config-test/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = EntityUtils.toString(response.getEntity());
            AssertUtils.assertTextContainsProperty(text, TEST_PROPERTY_NAME, TEST_PROPERTY_VALUE);

            // Step 2: Remove config-source at RUNTIME (no reload)
            ModelNode op = new ModelNode();
            op.get("address").add("subsystem", "microprofile-config-smallrye");
            op.get("address").add("config-source", "runtime-test-source");
            op.get("operation").set("remove");

            ModelNode result = controllerClient.execute(op);
            Assert.assertEquals("success", result.get("outcome").asString());

            // Step 3: Query again - property should be gone
            // PHASE 1 EXPECTATION: This will FAIL - property still returned (stale Config instance)
            // PHASE 2 EXPECTATION: This will PASS - property not found (Config instance refreshed)
            response = client.execute(new HttpGet(url + "runtime-config-test/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            text = EntityUtils.toString(response.getEntity());

            // Assert property is gone (should show default value "NOT_FOUND")
            AssertUtils.assertTextContainsProperty(text, TEST_PROPERTY_NAME, "NOT_FOUND");
        }
    }
}
