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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Kabir Khan
 */
public class InstallerContentPreparer {
    public static void main(String[] args) throws Exception {
        Path srcDir = getThisMavenModuleDirectory();

        // The dist module contains the MP modules. In EAP they are in a separate 'microprofile' layer
        Path distModulesDirectory = getMavenModuleTargetServerDirectory("dist", srcDir, true);
        // The ee-dist module does not contain the MP modules
        Path eedistModulesDirectory = getMavenModuleTargetServerDirectory("ee-dist", srcDir, false);

        List<Path> mpModules = diffModulesDirectories(eedistModulesDirectory, distModulesDirectory);

        // overrides.txt contains names of modules used by the observability layer which should override those modules
        // in older target distributions
        List<Path> overrides = loadOverrides();
        mpModules.addAll(overrides);

        Path installerModulesDir = createInstallerModulesDir(srcDir);
        copyModulesToInstaller(distModulesDirectory, installerModulesDir, mpModules);
    }

    private static Path getThisMavenModuleDirectory() throws Exception {
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

    private static Path getMavenModuleTargetServerDirectory(String mavenModule, Path srcDir, boolean checkMicroProfileLayer) throws Exception {
        String distName = System.getProperty("mp.installer.dist.name");
        if (distName == null) {
            throw new IllegalStateException("-Dmp.installer.dist.name not set");
        }
        List<Path> paths = new ArrayList<>();
        if (checkMicroProfileLayer) {
            paths.add(srcDir.resolveSibling(mavenModule + "/target/" + distName + "/modules/system/layers/microprofile"));
        }
        paths.add(srcDir.resolveSibling(mavenModule + "/target/" + distName + "/modules/system/layers/base"));

        Path path = null;
        for (Path curr : paths) {
            if (Files.exists(curr)) {
                return curr;
            }
        }

        throw new IllegalStateException("Could not find any of the following paths: " + paths);
    }

    private static List<Path> diffModulesDirectories(Path nonMPModulesDir, Path mpModulesDir) throws Exception {
        Map<Path, ModuleInfo> nonMPModules = loadModuleInfos(nonMPModulesDir);
        Map<Path, ModuleInfo> mpModules = loadModuleInfos(mpModulesDir);
        List<Path> result = new ArrayList<>();

        for (Map.Entry<Path, ModuleInfo> entry : mpModules.entrySet()) {
            ModuleInfo nonMp = nonMPModules.get(entry.getKey());
            if (nonMp == null) {
                result.add(entry.getKey());
                continue;
            }
            if (!nonMp.equals(entry.getValue())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    private static Map<Path, ModuleInfo> loadModuleInfos(Path modulesDir) throws Exception {
        Map<Path, ModuleInfo> infos = new HashMap<>();
        Files.walkFileTree(modulesDir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (file.getFileName().toString().equals("module.xml")) {
                    ModuleInfo info = ModuleInfo.create(modulesDir, file);
                    infos.put(info.getRelativePath(), info);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return infos;
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
