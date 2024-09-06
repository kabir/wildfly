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

package org.wildfly.test.integration.microprofile.reactive.messaging.inmemory;

import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
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

import java.util.List;
import java.util.PropertyPermission;

import static org.jboss.as.test.shared.integration.ejb.security.PermissionUtils.createPermissionsXmlAsset;

/**
 * Run with
 * mvn clean install -DallTests -pl testsuite/integration/microprofile -Dtest=ReactiveMessagingInMemoryUserApiTestCase
 */
@RunWith(Arquillian.class)
@ServerSetup({EnableReactiveExtensionsSetupTask.class})
public class ReactiveMessagingInMemoryTestCase {

    @Inject
    InMemoryBean inMemoryBean;

    @Inject
    @Any
    InMemoryConnector connector;

    @Deployment
    public static WebArchive getDeployment() {

        return ShrinkWrap.create(WebArchive.class, "reactive-messaging-connector-inmemory.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(ReactiveMessagingInMemoryTestCase.class.getPackage())
                .addClasses(EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsWebInfResource(ReactiveMessagingInMemoryTestCase.class.getPackage(), "microprofile-config.properties", "classes/META-INF/microprofile-config.properties")
                .addClass(TimeoutUtil.class)
                .addAsManifestResource(createPermissionsXmlAsset(
                        new PropertyPermission(TimeoutUtil.FACTOR_SYS_PROP, "read")
                ), "permissions.xml");
    }

    @Test
    public void testIncomingMessageSendFromInMemoryConnector() throws InterruptedException {
        connector.source("from-inmemory").send("IncomingTestMessage");
        Assert.assertEquals("IncomingTestMessage",inMemoryBean.getMessage());
    }

    @Test
    public void testOutgoingMessageReceivedByInMemoryConnector() throws InterruptedException {
        inMemoryBean.outgoing("OutgoingTestMessage");
        List<? extends Message<Object>> messages = connector.sink("to-inmemory").received();
        Assert.assertEquals(1,messages.size());
        Assert.assertEquals("OutgoingTestMessage",messages.get(0).getPayload());
    }

}
