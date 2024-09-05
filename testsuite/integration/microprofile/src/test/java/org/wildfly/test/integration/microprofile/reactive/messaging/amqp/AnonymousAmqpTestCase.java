/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.amqp;

import static org.awaitility.Awaitility.await;
import static org.jboss.as.test.shared.PermissionUtils.createPermissionsXmlAsset;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyPermission;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.MediaType;

import io.restassured.response.Response;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;
import org.wildfly.test.integration.microprofile.reactive.RunArtemisAmqpSetupTask;

import io.restassured.RestAssured;

@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({RunArtemisAmqpSetupTask.class, EnableReactiveExtensionsSetupTask.class})
public class AnonymousAmqpTestCase {
    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive createDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "reactive-messaging-anonymous-amqp.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setWebXML(AnonymousAmqpTestCase.class.getPackage(), "web.xml")
                .addClasses(ConsumingBean.class, ProducingBean.class, TestResource.class, ProducingBean.class)
                .addClasses(RunArtemisAmqpSetupTask.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(AnonymousAmqpTestCase.class.getPackage(), "microprofile-config-no-ssl.properties", "classes/META-INF/microprofile-config.properties")
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

    @Test
    public void testEmitter() throws Exception {
        postMessage("one");

        List<String> list = getReceived();
        if (list.size() == 0) {
            // Occasionally we might start sending messages before the subscriber is connected property
            // (the connection happens async as part of the application start) so retry until we get this first message
            Thread.sleep(1000);
            long end = System.currentTimeMillis() + 20000;
            while (true) {
                list = getReceived();
                if (getReceived().size() != 0) {
                    break;
                }

                if (System.currentTimeMillis() > end) {
                    break;
                }
                postMessage("one");
                Thread.sleep(1000);
            }
        }


        postMessage("two");

        long end = System.currentTimeMillis() + 20000;
        while (list.size() != 2 && System.currentTimeMillis() < end) {
            list = getReceived();
            Thread.sleep(1000);
        }
        waitUntilListPopulated(20000, "one", "two");
    }

    private void waitUntilListPopulated(long timoutMs, String... expected) throws Exception {
        List<String> list = new ArrayList<>();
        long end = System.currentTimeMillis() + timoutMs;
        while (list.size() < expected.length && System.currentTimeMillis() < end) {
            list = getReceived();
            Thread.sleep(1000);
        }
        Assert.assertArrayEquals(expected, list.toArray(new String[list.size()]));
    }

    private List<String> getReceived() throws Exception {
        Response r = RestAssured.get(url + "last/emitter");
        Assert.assertEquals(200, r.getStatusCode());
        return r.as(List.class);
    }

    private void postMessage(String s) throws Exception {
        int status = RestAssured.given().header("Content-Type", MediaType.TEXT_PLAIN).post(url).getStatusCode();
        Assert.assertEquals(200, status);
    }}
