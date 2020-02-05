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
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public class ModuleInfo {
    private final Path relativePath;
    private final Set<String> resourceNames;
    private final long allContentsHash;

    public ModuleInfo(Path relativePath, Set<String> resourceNames, long allContentsHash) {
        this.relativePath = relativePath;
        this.resourceNames = resourceNames;
        this.allContentsHash = allContentsHash;
    }

    public Path getRelativePath() {
        return relativePath;
    }

    public static ModuleInfo create(Path modulesDir, Path moduleXmlPath) throws IOException {
        final Path directory = moduleXmlPath.getParent();
        final Path relativePath = modulesDir.relativize(directory);
        final Set<String> resourceNames = new HashSet<>();

        AtomicLong hash = new AtomicLong(0);
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                String relative = modulesDir.relativize(file).toString();

                long fileHash = getFileHash(relative, file);

                 // XOR is order independent
                hash.set(hash.get() ^ fileHash);

                resourceNames.add(relative);

                return FileVisitResult.CONTINUE;
            }
        });
        return new ModuleInfo(relativePath, Collections.unmodifiableSet(resourceNames), hash.get());
    }

    private static long getFileHash(String name, Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(Files.readAllBytes(file));
            return Arrays.hashCode(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleInfo that = (ModuleInfo) o;
        return allContentsHash == that.allContentsHash &&
                relativePath.equals(that.relativePath) &&
                resourceNames.equals(that.resourceNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(relativePath, resourceNames, allContentsHash);
    }
}
