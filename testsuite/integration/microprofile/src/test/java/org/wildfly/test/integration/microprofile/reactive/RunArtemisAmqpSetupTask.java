/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.reactive;

import org.apache.activemq.artemis.core.security.CheckType;
import org.apache.activemq.artemis.core.security.Role;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Setup task to start an embedded version of Artemis for AMQP support.
 * I don't want to use the subsystem one because it means needing to run standalone-full.xml. Also, product does not
 * include the AMQP protocol, and would need the protocol module added which in turn would mean adjustments
 * of other module.xml files.
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunArtemisAmqpSetupTask implements ServerSetupTask {
    private static volatile EmbeddedActiveMQ server;
    private volatile String brokerXml = "messaging/amqp/broker.xml";
    private volatile Path replacedBrokerXml;

    public RunArtemisAmqpSetupTask() {
    }

    public RunArtemisAmqpSetupTask(String brokerXml) {
        this.brokerXml = brokerXml;
    }

    @Override
    public void setup(ManagementClient managementClient, String containerId) throws Exception {
        try {
            server = new EmbeddedActiveMQ();
            URL url = RunArtemisAmqpSetupTask.class.getResource(brokerXml);
            Path path = Paths.get(url.toURI());
            List<String> lines = Files.readAllLines(path);

            // The broker.xml expects an absolute path to the server keystore. So copy it to a new location,
            // and replace the '$SERVER_KEYSTORE$' placeholder with the actual location
            if (!Files.exists(KeystoreUtil.KEY_STORE_DIRECTORY_PATH)) {
                Files.createDirectories(KeystoreUtil.KEY_STORE_DIRECTORY_PATH);
            }
            replacedBrokerXml = Files.createTempFile(KeystoreUtil.KEY_STORE_DIRECTORY_PATH, "broker", "xml");
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(replacedBrokerXml.toFile()))) {
                String serverKeystorePath = KeystoreUtil.SERVER_KEYSTORE_PATH.toAbsolutePath().normalize().toString();
                // On Windows this will parse to e.g: C:\some\where. The Artemis broker.xml parser does not like this
                // so attempt to change this to C:/some/where
                serverKeystorePath = serverKeystorePath.replaceAll("\\\\", "/");

                for (String line : lines) {
                    String replaced = line.replace("$SERVER_KEYSTORE$", serverKeystorePath);
                    writer.write(replaced);
                    writer.newLine();
                }
            }
            String brokerXml = replacedBrokerXml.toUri().toURL().toExternalForm();

            server.setConfigResourcePath(brokerXml);
            server.setSecurityManager(new ActiveMQSecurityManager() {
                @Override
                public boolean validateUser(String username, String password) {
                    return true;
                }

                @Override
                public boolean validateUserAndRole(String username, String password, Set<Role> set, CheckType checkType) {
                    return true;
                }
            });
            server.start();
            await().atMost(60, SECONDS).until(() -> server.getActiveMQServer().isStarted());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
        try {
            if (server != null) {
                server.stop();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (Files.exists(replacedBrokerXml)) {
                Files.delete(replacedBrokerXml);
            }
        }
    }
}
