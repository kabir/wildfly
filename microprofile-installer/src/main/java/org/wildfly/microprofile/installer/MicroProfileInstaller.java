/*
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2020, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.microprofile.installer;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MicroProfileInstaller {
    private final Path modulesDir;

    public MicroProfileInstaller(Path modulesDir) {
        this.modulesDir = modulesDir;
    }


    public static void main(String[] args) throws Exception {
        URL url = MicroProfileInstaller.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.toString().contains(".jar")) {
            throw new IllegalStateException("The MicroProfile Installer must be run from the distributed jar. It should not be unzipped!");
        }
        ArgsParser parser = new ArgsParser(url, args);
        parser.parseArguments();

        Path tempDir = Files.createTempDirectory("wildflY-mp-installer");
        try {
            System.out.println(tempDir);
            Path srcModulesDir = unzipSelfToTemp(url, tempDir);
            String layer = readTargetLayerFromManifest();
            Path destLayerDir = setupLayerRoot(parser, layer);

            copyModules(srcModulesDir, destLayerDir);
        } finally {
            Files.walkFileTree(tempDir, new FileVisitors.DeleteDirectory());
        }
    }

    private static Path unzipSelfToTemp(URL url, Path tempDir) throws Exception {
        byte[] buffer = new byte[1024];
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(new File(url.toURI()))))) {
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                try {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    Path path = tempDir.resolve(entry.getName());
                    Path dir = path.getParent();
                    Files.createDirectories(dir);

                    try (OutputStream out = new BufferedOutputStream(new FileOutputStream(path.toFile()))) {
                        int len;
                        while ((len = zin.read(buffer)) > 0) {
                            out.write(buffer, 0, len);
                        }
                    }
                } finally {
                    zin.closeEntry();
                    entry = zin.getNextEntry();
                }
            }
        }

        return tempDir.resolve("modules");
    }

    private static String readTargetLayerFromManifest() throws Exception {
        String layer = readValueFromManifest("wildfly-target-layer");
        if ("base".equals(layer)) {
            layer = null;
        }
        return layer;
    }

    private static String readValueFromManifest(String name) throws Exception {
        String value;
        try (InputStream stream = MicroProfileInstaller.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
            Manifest manifest = null;
            if (stream != null) {
                manifest = new Manifest(stream);
            }
            value = manifest.getMainAttributes().getValue(name);
        }
        return value;

    }

    private static Path setupLayerRoot(ArgsParser parser, String layer) throws Exception {
        Path layersRoot = parser.modulesDir.resolve("system/layers");
        if (!Files.exists(layersRoot)) {
            throw new IllegalStateException("No " + layersRoot + " directory was found");
        }

        if (layer != null) {
            Path layersConf = parser.modulesDir.resolve("layers.conf");
            List<String> layerConfContents = null;
            if (!Files.exists(layersConf)) {
                layerConfContents = Collections.singletonList("layers=" + layer);
            } else {
                layerConfContents = addLayersConfEntry(layersConf, layer);
            }

            Files.write(layersConf, layerConfContents);
            Path layerDir = layersRoot.resolve(layer);
            Files.createDirectories(layerDir);
            return layerDir;
        }

        Path layerDir = layersRoot.resolve("base");
        if (!Files.exists(layerDir)) {
            throw new IllegalStateException("No " + layerDir + " directory was found");
        }
        return layerDir;
    }

    private static List<String> addLayersConfEntry(Path layersConf, String layer) throws Exception {
        List<String> lines = Files.readAllLines(layersConf);
        for (int i = 0 ; i < lines.size() ; i++) {
            String line = lines.get(i);
            if (line.trim().startsWith("layers=")) {
                List<String> tokens = Arrays.asList(line.substring(7).split(","));
                boolean layerAlreadyExists = false;
                for (String s : tokens) {
                    if (s.trim().equals(layer)) {
                        layerAlreadyExists = true;
                        break;
                    }
                }

                if (!layerAlreadyExists) {
                    int baseIndex = -1;
                    for (int j = 0; j < tokens.size(); j++) {
                        if (tokens.get(j).trim().equals("base")) {
                            baseIndex = j;
                            break;
                        }
                    }

                    if (baseIndex != -1) {
                        tokens.add(baseIndex, layer);
                    } else {
                        tokens.add(layer);
                    }

                    StringBuilder lineBuilder = new StringBuilder("layers=");
                    boolean first = true;
                    for (String s : tokens) {
                        if (!first) {
                            lineBuilder.append(",");
                        } else {
                            first = false;
                        }
                        lineBuilder.append(s);
                    }
                    lines.set(i, lineBuilder.toString());
                }

            }
        }
        return lines;
    }

    private static void copyModules(Path srcDir, Path targetDir) throws Exception {
        System.out.println("Copying modules from " + srcDir + " to " + targetDir);
        Files.walkFileTree(srcDir, new FileVisitors.CopyDirectory(srcDir, targetDir));
    }

    private static class ArgsParser {
        private URL url;
        private final String[] args;
        private Path modulesDir;

        private ArgsParser(URL url, String[] args) {
            this.url = url;
            this.args = args;
        }

        private void parseArguments() throws Exception {
            String jbossHome = null;
            String modulesDir = null;

            for (String arg : args) {
                if (arg.equals("--help")) {
                    displayHelp();
                    System.exit(0);
                } else if (arg.startsWith("--home=")) {
                    jbossHome = arg.substring(7);
                } else if (arg.startsWith("--modules=")) {
                    modulesDir = arg.substring(10);
                }
            }

            if (modulesDir != null && jbossHome != null) {
                System.err.println("Both --home and --modules were used!\n");
                displayHelp();
            }

            if (modulesDir != null) {
                this.modulesDir = Paths.get(modulesDir);
            } else {
                if (jbossHome == null) {
                    jbossHome = System.getenv("JBOSS_HOME");
                }
                this.modulesDir = Paths.get(jbossHome, "modules");
            }
            System.out.println(jbossHome);
            System.out.println(modulesDir);
            if (!Files.exists(this.modulesDir)) {
                throw new IllegalStateException("The determined modules base directory " + this.modulesDir + " does not exist");
            }
            if (!Files.exists(this.modulesDir.resolve("system"))) {
                throw new IllegalStateException("The determined modules base directory " + this.modulesDir + " does not look like a modules directory");
            }
        }

        private void displayHelp() throws Exception {
            System.out.println("Usage:");
            Path path = Paths.get(url.toURI());
            System.out.println("java -jar " + path.getFileName().toString() + "[args]");
            System.out.println("where args include:");
            System.out.println();
            System.out.println("--help");
            System.out.println("\tDisplay this help message");
            System.out.println();
            System.out.println("--home=<value>");
            System.out.println("\tPoints to the root of the server you want to patch. If not present this may also be " +
                    "set in the JBOSS_HOME environment variable. If --modules is not set, we use the modules/ directory" +
                    "under this location as the path for the modules to add the MicroProfile functionality to");
            System.out.println();
            System.out.println("--modules=<value>");
            System.out.println("\tPoints to the modules directory to add the MicroProfile functionality to." +
                    "If present, it overrides any value determined from --home or JBOSS_HOME.");
            System.out.println();
            System.out.println("If no modules directory can be determined this message is shown");
        }

    }
}
