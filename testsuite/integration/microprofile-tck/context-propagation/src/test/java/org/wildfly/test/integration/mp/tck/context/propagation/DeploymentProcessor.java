/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.mp.tck.context.propagation;

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