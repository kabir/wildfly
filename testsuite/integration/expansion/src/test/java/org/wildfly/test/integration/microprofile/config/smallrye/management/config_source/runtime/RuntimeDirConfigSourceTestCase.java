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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;

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

/**
 * Test adding a directory-based config-source at RUNTIME using management operations.
 *
 * This test validates Phase 1 of WFLY-21615:
 * - Create a temp directory with property files before test
 * - Deploy application first
 * - Add dir-based config-source at runtime via management operation
 * - Verify properties from the directory are visible in the application
 *
 * Note: This test will FAIL until Phase 2 implementation is complete, as runtime
 * addition of directory-based config-sources is not yet implemented.
 *
 * @author Claude Code (Phase 1 test for WFLY-21615)
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(RuntimeDirConfigSourceTestCase.RuntimeDirConfigSourceSetupTask.class)
public class RuntimeDirConfigSourceTestCase extends AbstractMicroProfileConfigTestCase {

    private static final String CONFIG_SOURCE_NAME = "runtime-test-dir";
    private static final String PROP_FROM_DIR_A = "prop-from-dir-a";
    private static final String PROP_FROM_DIR_B = "prop-from-dir-b";
    private static final String VALUE_FROM_DIR_A = "value-a";
    private static final String VALUE_FROM_DIR_B = "value-b";

    @Deployment(testable = false)
    public static Archive<?> deploy() {
        WebArchive war = ShrinkWrap.create(WebArchive.class, "RuntimeDirConfigSourceTestCase.war")
                .addClasses(RuntimeDirTestApplication.class)
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        return war;
    }

    @ArquillianResource
    private URL url;

    /**
     * Test that properties from a runtime-added directory-based config-source are visible.
     *
     * Expected behavior (once Phase 2 is implemented):
     * 1. Application deployed without config-source
     * 2. Directory with property files created before test
     * 3. Config-source added at runtime via management operation pointing to directory
     * 4. Properties from directory files should be accessible via MicroProfile Config API
     */
    @Test
    public void testRuntimeAddedDirConfigSource() throws Exception {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpResponse response = client.execute(new HttpGet(url + "runtime-dir-config-test/test"));
            Assert.assertEquals(200, response.getStatusLine().getStatusCode());
            String text = EntityUtils.toString(response.getEntity());

            System.out.println("Runtime dir config-source test response:");
            System.out.println(text);

            // Verify properties from runtime-added directory config-source are visible
            AssertUtils.assertTextContainsProperty(text, PROP_FROM_DIR_A, VALUE_FROM_DIR_A);
            AssertUtils.assertTextContainsProperty(text, PROP_FROM_DIR_B, VALUE_FROM_DIR_B);

            System.out.println("SUCCESS: Runtime directory config-source properties are accessible!");
        }
    }

    /**
     * Setup task that creates a temp directory with property files at server startup
     * and adds the dir config-source at runtime (after deployment).
     */
    static class RuntimeDirConfigSourceSetupTask implements ServerSetupTask {

        private volatile Path tempDir;

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            // Create temp directory with property files
            Path target = Paths.get("target").toAbsolutePath().normalize();
            tempDir = Files.createTempDirectory(target, "runtime-test-dir");
            Assert.assertTrue(Files.exists(tempDir));

            // Create property files in the directory
            createPropertyFile(tempDir, PROP_FROM_DIR_A, VALUE_FROM_DIR_A);
            createPropertyFile(tempDir, PROP_FROM_DIR_B, VALUE_FROM_DIR_B);

            System.out.println("Created temp directory for runtime test: " + tempDir);
            System.out.println("  - " + PROP_FROM_DIR_A + " = " + VALUE_FROM_DIR_A);
            System.out.println("  - " + PROP_FROM_DIR_B + " = " + VALUE_FROM_DIR_B);

            // Add dir config-source at RUNTIME (after deployment would have occurred)
            addDirConfigSourceAtRuntime(managementClient.getControllerClient());
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            // Remove the runtime-added config-source
            removeConfigSource(managementClient.getControllerClient());

            // Clean up temp directory
            deleteDirectory(tempDir);
        }

        /**
         * Create a property file in the directory.
         * File name is the property name, content is the property value.
         */
        private void createPropertyFile(Path dir, String propertyName, String propertyValue) throws IOException {
            Path file = dir.resolve(propertyName);
            Files.createFile(file);
            Assert.assertTrue(Files.exists(file));
            Files.write(file, Collections.singletonList(propertyValue));
        }

        /**
         * Add a directory-based config-source at runtime using management operations.
         * This simulates the runtime scenario being tested.
         */
        private void addDirConfigSourceAtRuntime(ModelControllerClient client) throws IOException {
            ModelNode op = new ModelNode();
            op.get(OP_ADDR).add(SUBSYSTEM, "microprofile-config-smallrye");
            op.get(OP_ADDR).add("config-source", CONFIG_SOURCE_NAME);
            op.get(OP).set(ADD);

            // Set dir attribute with path
            ModelNode dirNode = new ModelNode();
            dirNode.get("path").set(escapePath(tempDir));
            op.get("dir").set(dirNode);

            ModelNode result = client.execute(op);

            // For debugging: print the result
            System.out.println("Add runtime dir config-source result: " + result.get("outcome").asString());
            if (!result.get("outcome").asString().equals("success")) {
                System.err.println("Failed to add runtime dir config-source: " + result.asString());
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

        /**
         * Escape path separators for cross-platform compatibility.
         */
        private String escapePath(Path path) {
            String s = path.toString();
            // Avoid problems with paths on Windows
            s = s.replace('\\', '/');
            return s;
        }

        /**
         * Recursively delete a directory and all its contents.
         */
        private void deleteDirectory(Path rootDir) throws IOException {
            if (rootDir != null && Files.exists(rootDir)) {
                Files.walkFileTree(rootDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return super.visitFile(file, attrs);
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return super.postVisitDirectory(dir, exc);
                    }
                });
            }
        }
    }
}
