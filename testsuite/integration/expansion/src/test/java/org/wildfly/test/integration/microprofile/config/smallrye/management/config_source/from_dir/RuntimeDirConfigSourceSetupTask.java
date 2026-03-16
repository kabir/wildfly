/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

/**
 * Setup task to create test directory and property file on the server side.
 */
public class RuntimeDirConfigSourceSetupTask implements ServerSetupTask {

    private static final String TEST_DIR_NAME = "test-config-dir";
    private static final String RUNTIME_PROPERTY_NAME = "runtime-test-property";
    private static final String RUNTIME_PROPERTY_VALUE = "runtime-dir-value";

    private Path testConfigDir;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        // Create directory in target (accessible to both client and server)
        Path target = Paths.get("target").toAbsolutePath().normalize();
        testConfigDir = Files.createTempDirectory(target, TEST_DIR_NAME);

        // Create property file (filename is the property name)
        Path propertyFile = testConfigDir.resolve(RUNTIME_PROPERTY_NAME);
        Files.write(propertyFile, RUNTIME_PROPERTY_VALUE.getBytes());
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        // Clean up directory and files
        if (testConfigDir != null && Files.exists(testConfigDir)) {
            Files.walk(testConfigDir)
                .sorted((a, b) -> b.compareTo(a)) // Delete files before directories
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                });
        }
    }

    public Path getTestConfigDir() {
        return testConfigDir;
    }
}
