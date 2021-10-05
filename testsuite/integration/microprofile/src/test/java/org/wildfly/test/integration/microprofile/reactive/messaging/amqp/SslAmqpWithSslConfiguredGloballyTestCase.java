/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.wildfly.test.integration.microprofile.reactive.messaging.amqp;

import io.restassured.RestAssured;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.ConfigureElytronSslContextSetupTask;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.KeystoreUtil;
import org.wildfly.test.integration.microprofile.reactive.RunArtemisAmqpSetupTask;

import java.net.URL;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({SslAmqpWithSslConfiguredGloballyTestCase.RunArtemisSslUsernamePasswordSecuredSetupTask.class, EnableReactiveExtensionsSetupTask.class, ConfigureElytronSslContextSetupTask.class})
public class SslAmqpWithSslConfiguredGloballyTestCase {
    @ArquillianResource
    URL url;

    @ArquillianResource
    private ManagementClient managementClient;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-kafka-user-api.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setWebXML(SslAmqpWithSslConfiguredGloballyTestCase.class.getPackage(), "web.xml")
                .addClasses(ConsumingBean.class, ProducingBean.class, TestResource.class)
                .addClasses(RunArtemisSslUsernamePasswordSecuredSetupTask.class, RunArtemisAmqpSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(SslAmqpWithSslConfiguredGloballyTestCase.class.getPackage(), "microprofile-config-ssl-global.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");

        return webArchive;
    }

    @Test
    public void test() {
        await().atMost(1, TimeUnit.MINUTES).until(() -> {
            String value = RestAssured.get(url + "last").asString();
            return value.equalsIgnoreCase(String.valueOf(ProducingBean.HIGH));
        });
    }

    static class RunArtemisSslUsernamePasswordSecuredSetupTask extends RunArtemisAmqpSetupTask {
        public RunArtemisSslUsernamePasswordSecuredSetupTask() {
            super("messaging/amqp/broker-ssl.xml");
        }

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            KeystoreUtil.createKeystores();
            super.setup(managementClient, containerId);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            try {
                super.tearDown(managementClient, containerId);
            } finally {
                KeystoreUtil.cleanUp();
            }
        }
    }
}
