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

import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Kabir Khan
 */
public class InstallerContentPreparer {
    public static void main(String[] args) throws Exception {
        Path srcDir = getThisModuleDirectory();
        Path galleonPackModulesDir = getGalleonPackModulesDirectory(srcDir);

        // Names of all directories in the MP Galleon Pack that contain a module.xml
        List<Path> mpModules = discoverMicroProfileModules(galleonPackModulesDir);

        // overrides.txt contains names of modules used by the observability layer which should override those modules
        // in older target distributions
        List<Path> overrides = loadOverrides();
        mpModules.addAll(overrides);

        Path distModulesDirectory = getDistModulesDirectory(srcDir);
        Path installerModulesDir = createInstallerModulesDir(srcDir);
        copyModulesToInstaller(distModulesDirectory, installerModulesDir, mpModules);
    }

    private static Path getThisModuleDirectory() throws Exception {
        URL url = InstallerContentPreparer.class.getProtectionDomain().getCodeSource().getLocation();
        Path path = Paths.get(url.toURI());
        Path tmp = path.getRoot();
        Path srcDir = null;
        for (Iterator<Path> it = path.iterator() ; it.hasNext() ; ) {
            Path curr = it.next();
            tmp = tmp.resolve(curr);
            if (curr.getFileName().toString().equals("microprofile-installer")) {
                srcDir = tmp.toAbsolutePath();
                break;
            }
        }

       if (srcDir == null) {
            throw new IllegalStateException("The MicroProfile diff util for the installer can only be run as part of a WildFly build");
        }
        if (!Files.exists(srcDir)) {
            throw new IllegalStateException(tmp +  " does not exist");
        }
        return srcDir;
    }

    private static Path getGalleonPackModulesDirectory(Path srcDir) {
        Path path = srcDir.resolveSibling("galleon-pack/src/main/resources/modules/system/layers");

        if (!Files.exists(path)) {
            throw new IllegalStateException("Could not find " + path.toFile().getAbsoluteFile());
        }

        Path modulesRoot = path.resolve("microprofile").normalize();
        if (!Files.exists(modulesRoot)) {
            Path mRoot = path.resolve("base").normalize();
            if (Files.exists(mRoot)) {
                modulesRoot = mRoot;
            } else {
                throw new IllegalStateException("Could not find " + modulesRoot.toFile().getAbsoluteFile() + " or "  + mRoot.toFile().getAbsoluteFile());
            }
        }
        return modulesRoot;
    }

    private static List<Path> loadOverrides() throws Exception {
        URL url = InstallerContentPreparer.class.getResource("overrides.txt");
        List<String> lines = Files.readAllLines(Paths.get(url.toURI()));
        List<Path> paths = new ArrayList<>();
        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            paths.add(Paths.get(line));
        }
        return paths;
    }

    private static Path getDistModulesDirectory(Path srcDir) throws Exception {
        String distName = System.getProperty("mp.installer.dist.name");
        if (distName == null) {
            throw new IllegalStateException("-Dmp.installer.dist.name not set");
        }
        Path path = srcDir.resolveSibling("dist/target/" + distName + "/modules/system/layers/base");

        if (!Files.exists(path)) {
            throw new IllegalStateException("Could not find " + path.toFile().getAbsoluteFile());
        }

        return path;
    }

    private static List<Path> discoverMicroProfileModules(Path modulesDir) throws Exception {
        List<Path> paths = new ArrayList<>();

        Files.walkFileTree(modulesDir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals("module.xml")) {
                    paths.add(modulesDir.relativize(file.getParent()));
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return paths;
    }

    private static Path createInstallerModulesDir(Path srcDir) throws Exception {
        Path path = srcDir.resolve("target/classes/modules");
        if (Files.exists(path)) {
            Files.walkFileTree(path, new FileVisitors.DeleteDirectory());
        }

        return Files.createDirectories(path);
    }

    private static void copyModulesToInstaller(Path distModulesDir, Path installerModulesDir, List<Path> relativeModules) throws Exception {
        Files.walkFileTree(installerModulesDir, new FileVisitors.DeleteDirectory());
        Files.createDirectories(installerModulesDir);

        for (Path relative : relativeModules) {
            Path fromDir = distModulesDir.resolve(relative);
            Path toDir = installerModulesDir.resolve(relative);

            Files.walkFileTree(fromDir, new FileVisitors.CopyDirectory(fromDir, toDir));

        }
    }

}
