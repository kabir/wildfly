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
import java.util.stream.Collectors;

class FileVisitors {
    static class CopyDirectory extends SimpleFileVisitor<Path> {
        private final Path src;
        private final Path dest;

        CopyDirectory(Path src, Path dest) {
            this.src = src;
            this.dest = dest;
        }

        @Override
        public FileVisitResult preVisitDirectory(final Path dir,
                                                 final BasicFileAttributes attrs) throws IOException {
            Path destDir = dest.resolve(src.relativize(dir));
            if (Files.exists(destDir)) {
                // If the directory already exists, delete all files
                for (Path p : Files.list(destDir).collect(Collectors.toList())) {
                    if (!Files.isDirectory(p)) {
                        Files.delete(p);
                    }
                }
            }
            Files.createDirectories(destDir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(final Path file,
                                         final BasicFileAttributes attrs) throws IOException {
            Files.copy(file, dest.resolve(src.relativize(file)));
            return FileVisitResult.CONTINUE;
        }
    }

    static class DeleteDirectory extends SimpleFileVisitor<Path> {
        @Override
        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            Files.delete(dir);
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.delete(file);
            return FileVisitResult.CONTINUE;
        }
    }

}
