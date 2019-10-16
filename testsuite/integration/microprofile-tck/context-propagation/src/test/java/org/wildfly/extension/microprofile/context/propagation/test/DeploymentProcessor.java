/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.context.propagation.test;

import java.io.File;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.jboss.arquillian.container.test.spi.client.deployment.ApplicationArchiveProcessor;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.IHookable;

import com.beust.jcommander.ParameterException;

public class DeploymentProcessor implements ApplicationArchiveProcessor {

    @Override
    public void process(Archive<?> archive, TestClass testClass) {
        if (archive instanceof WebArchive) {
            JavaArchive extensionsJar = ShrinkWrap.create(JavaArchive.class, "extension.jar");

            addAllClassesInJar(extensionsJar, IHookable.class);

            extensionsJar.addClass(Parameter.class);
            extensionsJar.addClass(ParameterException.class);

            WebArchive war = WebArchive.class.cast(archive);
            war.addAsLibraries(extensionsJar);

            war.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
        }
    }

    private void addAllClassesInJar(JavaArchive archive, Class<?> clazz) {
        // Some jar files do not have entries for the package, and for those Archive.addPackage() does not work
        // An example of a working jar is:
        //
        // ---------
        // $unzip -l /Users/kabir/.m2/repository/org/jboss/arquillian/testng/arquillian-testng-container/1.4.1.Final/arquillian-testng-container-1.4.1.Final.jar
        // Archive:  /Users/kabir/.m2/repository/org/jboss/arquillian/testng/arquillian-testng-container/1.4.1.Final/arquillian-testng-container-1.4.1.Final.jar
        // Length      Date      Time    Name
        // ---------  ---------- -----   ----
        // 0          10-19-2018 12:06   org/jboss/arquillian/testng/container/
        // 3099       10-19-2018 12:06   org/jboss/arquillian/testng/container/TestNGTestRunner.class
        // ---------
        //
        // 'Broken' jars just list the class entries and not the folder names

        URL url = clazz.getProtectionDomain().getCodeSource().getLocation();
        JarFile file = null;
        try {
            file = new JarFile(new File(url.toURI()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // String pkgPrefix = clazz.getPackage().getName().replace(".", "/") + "/";
        for (Enumeration<JarEntry> e = file.entries(); e.hasMoreElements(); ) {
            String name = e.nextElement().getName();
            if (name.endsWith(".class")) {
                // Get rid of the .class suffix
                String className = name.substring(0, name.length() - 6);
                className = className.replace("/", ".");
                try {
                    archive.addClass(className);
                } catch (NoClassDefFoundError ignore) {
                    // Just ignore this. It seems to mainly happen for some obscure internal classes
                }
            }
        }
    }
}