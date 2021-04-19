/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2020, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.jboss.as.arquillian.api.ServerSetupTask;
import org.jboss.as.arquillian.container.ManagementClient;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class RunKafkaSetupTask implements ServerSetupTask {

    public static String NUM_PARTITIONS = "org.wildfly.test.kafka.num.partitions";


    private static volatile EmbeddedKafkaBroker broker;
    private volatile Path kafkaDir;

    @Override
    public void setup(ManagementClient managementClient, String s) throws Exception {

        int numPartitions = Integer.valueOf(System.getProperty(NUM_PARTITIONS, "1"));

        Path target = Paths.get("target").toAbsolutePath().normalize();
        kafkaDir = Files.createTempDirectory(target, "kafka");

        Files.createDirectories(kafkaDir);

        broker = new EmbeddedKafkaBroker(1, true, "testing")
                .zkPort(2181)
                .kafkaPorts(9092)
                .brokerProperty("log.dir", kafkaDir.toString())
                .brokerProperty("num.partitions", 1)
                .brokerProperty("offsets.topic.num.partitions", numPartitions);

        broker.afterPropertiesSet();
    }

    @Override
    public void tearDown(ManagementClient managementClient, String s) throws Exception {
        try {
            if (broker != null) {
                broker.destroy();
            }
        } finally {
            try {
                Files.walkFileTree(kafkaDir, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!Files.isDirectory(file)) {
                            Files.delete(file);
                        }
                        return super.visitFile(file, attrs);
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return super.postVisitDirectory(dir, exc);
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static EmbeddedKafkaBroker getEmbeddedKafka() {
        return broker;
    }
}
