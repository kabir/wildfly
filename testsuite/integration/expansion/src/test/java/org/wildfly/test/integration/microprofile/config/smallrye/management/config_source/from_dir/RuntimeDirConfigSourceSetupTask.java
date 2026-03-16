/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_dir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

/**
 * Setup task to create test directory and property file on the server side.
 */
public class RuntimeDirConfigSourceSetupTask implements ServerSetupTask {

    private static final String TEST_DIR_NAME = "test-config-dir";
    private static final String RUNTIME_PROPERTY_NAME = "runtime.test.property";
    private static final String RUNTIME_PROPERTY_VALUE = "runtime-dir-value";

    private File testConfigDir;

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        // Create directory on server filesystem
        String configDir = System.getProperty("jboss.server.config.dir");
        testConfigDir = new File(configDir, TEST_DIR_NAME);

        if (!testConfigDir.exists()) {
            testConfigDir.mkdirs();
        }

        // Create property file
        File propertyFile = new File(testConfigDir, RUNTIME_PROPERTY_NAME);
        try (FileWriter writer = new FileWriter(propertyFile)) {
            writer.write(RUNTIME_PROPERTY_VALUE);
        }
    }

    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        // Clean up directory and files
        if (testConfigDir != null && testConfigDir.exists()) {
            File[] files = testConfigDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    Files.deleteIfExists(file.toPath());
                }
            }
            Files.deleteIfExists(testConfigDir.toPath());
        }
    }
}
