/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.test.integration.microprofile.reactive;

import org.jboss.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class KeystoreUtil {
    private static final Logger log = Logger.getLogger(KeystoreUtil.class.getSimpleName());

    private static final String KEY_STORE_DIRECTORY = "target/reactive-messaging-security";
    public static final Path KEY_STORE_DIRECTORY_PATH = Paths.get(KEY_STORE_DIRECTORY);
    public static final String SERVER_KEYSTORE = KEY_STORE_DIRECTORY + "/server.keystore.p12";
    public static final Path SERVER_KEYSTORE_PATH = Paths.get(SERVER_KEYSTORE);
    private static final String SERVER_CER = KEY_STORE_DIRECTORY + "/server.cer";
    private static final String SERVER_TRUSTSTORE = KEY_STORE_DIRECTORY + "/server.truststore.p12";
    public static final String CLIENT_TRUSTSTORE = KEY_STORE_DIRECTORY + "/client.truststore.p12";
    public static final String KEYSTORE_PWD = "serverks";
    private static final String SERVER_TRUSTSTORE_PWD = "serverts";
    public static final String CLIENT_TRUSTSTORE_PWD = "clientts";

    public static void createKeystores() throws IOException {
        Files.createDirectories(KEY_STORE_DIRECTORY_PATH);

        //keytool -genkeypair -alias localhost -keyalg RSA -keysize 2048 -storetype PKCS12 -keystore server.keystore.p12 -validity 3650  -ext SAN=DNS:localhost,IP:127.0.0.1
        final List<String> createKeyStoreWithCertificateCommand = new ArrayList<>(List.of("keytool", "-genkeypair"));
        createKeyStoreWithCertificateCommand.addAll(List.of("-alias", "localhost"));
        createKeyStoreWithCertificateCommand.addAll(List.of("-keyalg", "RSA"));
        createKeyStoreWithCertificateCommand.addAll(List.of("-keysize", "2048"));
        createKeyStoreWithCertificateCommand.addAll(List.of("-storetype", "PKCS12"));
        createKeyStoreWithCertificateCommand.addAll(List.of("-keystore", SERVER_KEYSTORE));
        createKeyStoreWithCertificateCommand.addAll(List.of("-storepass", KEYSTORE_PWD));
        createKeyStoreWithCertificateCommand.addAll(List.of("-keypass", KEYSTORE_PWD));
        createKeyStoreWithCertificateCommand.addAll(
                List.of("-dname", "CN=localhost, OU=Unknown, O=Unknown, L=Unknown, ST=Unknown, C=Unknown"));
        createKeyStoreWithCertificateCommand.addAll(List.of("-validity", "3650"));
        createKeyStoreWithCertificateCommand.addAll(List.of("-ext", "SAN=DNS:localhost,IP:127.0.0.1"));
        runKeytoolCommand(createKeyStoreWithCertificateCommand);

        //keytool -exportcert -alias localhost -keystore server.keystore.p12 -file server.cer -storetype pkcs12 -noprompt -storepass serverks
        final List<String> exportCertificateCommand = new ArrayList<>(List.of("keytool", "-exportcert"));
        exportCertificateCommand.addAll(List.of("-alias", "localhost"));
        exportCertificateCommand.addAll(List.of("-keystore", SERVER_KEYSTORE));
        exportCertificateCommand.addAll(List.of("-file", SERVER_CER));
        exportCertificateCommand.addAll(List.of("-storetype", "PKCS12"));
        exportCertificateCommand.addAll(List.of("-noprompt"));
        exportCertificateCommand.addAll(List.of("-storepass", KEYSTORE_PWD));
        runKeytoolCommand(exportCertificateCommand);

        //keytool -keystore server.truststore.p12 -alias localhost -importcert -file server.cer -storetype pkcs12
        final List<String> importServerTrustStoreCommand = new ArrayList<>(List.of("keytool"));
        importServerTrustStoreCommand.addAll(List.of("-keystore", SERVER_TRUSTSTORE));
        importServerTrustStoreCommand.addAll(List.of("-alias", "localhost"));
        importServerTrustStoreCommand.addAll(List.of("-importcert"));
        importServerTrustStoreCommand.addAll(List.of("-file", SERVER_CER));
        importServerTrustStoreCommand.addAll(List.of("-storetype", "PKCS12"));
        importServerTrustStoreCommand.addAll(List.of("-storepass", SERVER_TRUSTSTORE_PWD));
        importServerTrustStoreCommand.addAll(List.of("-keypass", SERVER_TRUSTSTORE_PWD));
        importServerTrustStoreCommand.addAll(List.of("-noprompt"));
        runKeytoolCommand(importServerTrustStoreCommand);

        //keytool -keystore client.truststore.p12 -alias localhost -importcert -file server.cer -storetype pkcs12
        final List<String> importClientTrustStoreCommand = new ArrayList<>(List.of("keytool"));
        importClientTrustStoreCommand.addAll(List.of("-keystore", CLIENT_TRUSTSTORE));
        importClientTrustStoreCommand.addAll(List.of("-alias", "localhost"));
        importClientTrustStoreCommand.addAll(List.of("-importcert"));
        importClientTrustStoreCommand.addAll(List.of("-file", SERVER_CER));
        importClientTrustStoreCommand.addAll(List.of("-storetype", "PKCS12"));
        importClientTrustStoreCommand.addAll(List.of("-storepass", CLIENT_TRUSTSTORE_PWD));
        importClientTrustStoreCommand.addAll(List.of("-keypass", CLIENT_TRUSTSTORE_PWD));
        importClientTrustStoreCommand.addAll(List.of("-noprompt"));
        runKeytoolCommand(importClientTrustStoreCommand);
    }

    private static void runKeytoolCommand(List<String> commandParameters) throws IOException {
        ProcessBuilder keytool = new ProcessBuilder().command(commandParameters);
        final Process process = keytool.start();

        try {
            process.waitFor();
        } catch (InterruptedException e) {
            log.errorf(e, "Keytool execution error");
        }

        log.debugf("Generating certificate using `keytool` using command: %s, parameters: %s", process.info(), commandParameters);

        if (process.exitValue() > 0) {
            final String processError = (new BufferedReader(new InputStreamReader(process.getErrorStream()))).lines()
                    .collect(Collectors.joining(" \\ "));
            final String processOutput = (new BufferedReader(new InputStreamReader(process.getInputStream()))).lines()
                    .collect(Collectors.joining(" \\ "));
            log.errorf("Error generating certificate, error output: %s, normal output: %s, commandline parameters: %s", processError, processOutput, commandParameters);
        }
    }

    public static void cleanUp() throws IOException {
        Files.walkFileTree(KEY_STORE_DIRECTORY_PATH, new SimpleFileVisitor<Path>() {
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
