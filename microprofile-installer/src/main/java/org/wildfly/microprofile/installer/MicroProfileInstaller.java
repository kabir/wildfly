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

package org.wildfly.microprofile.installer;

import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class MicroProfileInstaller {
    public static void main(String[] args) throws Exception {
        URL url = MicroProfileInstaller.class.getProtectionDomain().getCodeSource().getLocation();
        if (!url.toString().contains(".jar")) {
            throw new IllegalStateException("The MicroProfile Installer must be run from the distributed jar. It should not be unzipped!");
        }

        Path tempDir = Files.createTempDirectory("wildflY-mp-installer");
        System.out.println(tempDir);
        byte[] buffer = new byte[1024];
        try (ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(new File(url.toURI()))))) {
            ZipEntry entry = zin.getNextEntry();
            while (entry != null) {
                System.out.println(entry);
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
    }
}
