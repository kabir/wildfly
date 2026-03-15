/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.reload;

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
 * Test that verifies WFLY-21616 fix: config-sources are properly initialized
 * before dependent subsystems on server reload (no race conditions).
 *
 * The SetupTask adds a config-source and triggers reload. This test verifies
 * that the config property is accessible, proving the service dependency works.
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(ConfigSourceReloadSetupTask.class)
public class ConfigSourceReloadRaceConditionTestCase extends AbstractMicroProfileConfigTestCase {

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        return ShrinkWrap.create(WebArchive.class, "ConfigSourceReloadRaceConditionTestCase.war")
                .addClasses(TestApplication.class, TestResource.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @ArquillianResource
    private URL url;

    @Test
    public void testConfigAvailableAfterReload() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + "custom-config/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = EntityUtils.toString(response.getEntity());
            // Verify the property from our config-source is accessible
            // If there's a race condition, this property would be missing
            AssertUtils.assertTextContainsProperty(text, "my.reload.test.property", "reload-test-value");
        }
    }
}
