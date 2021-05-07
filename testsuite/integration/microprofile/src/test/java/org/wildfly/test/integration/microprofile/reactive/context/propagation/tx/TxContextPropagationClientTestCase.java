/*
 * Copyright 2020 Red Hat, Inc.
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

package org.wildfly.test.integration.microprofile.reactive.context.propagation.tx;

import static org.hamcrest.CoreMatchers.equalTo;

import java.io.File;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.awaitility.Awaitility;
import org.awaitility.core.ThrowingRunnable;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;

import io.restassured.RestAssured;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup({EnableReactiveExtensionsSetupTask.class})
public class TxContextPropagationClientTestCase {

    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "ctx-ppgn-endpoint.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setWebXML(TxContextPropagationClientTestCase.class.getPackage(), "web.xml")
                .addAsWebInfResource(TxContextPropagationClientTestCase.class.getPackage(), "persistence.xml", "classes/META-INF/persistence.xml")
                .addClasses(EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addPackage(TxContextPropagationClientTestCase.class.getPackage())
                // TODO add to deployment unit dependencies?
                //.addAsResource(new StringAsset("Dependencies: io.reactivex.rxjava2.rxjava,org.eclipse.microprofile.reactive-streams-operators.api\n"), "META-INF/MANIFEST.MF");
                .addAsResource(new StringAsset("Dependencies: io.reactivex.rxjava2.rxjava\n"), "META-INF/MANIFEST.MF");

        System.out.println(webArchive.toString(true));
        webArchive.as(ZipExporter.class).exportTo(new File("target/" + webArchive.getName()), true);
        return webArchive;
    }

    @Test
    public void testTx() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction1").then()
                .statusCode(Response.Status.OK.getStatusCode());
        RestAssured.when().get(url.toExternalForm() + "context/transaction2").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
        RestAssured.when().get(url.toExternalForm() + "context/transaction3").then()
                .statusCode(Response.Status.CONFLICT.getStatusCode());
        RestAssured.when().get(url.toExternalForm() + "context/transaction-delete1").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testTransactionTCContextPropagation() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction-tc").then()
                .statusCode(Response.Status.OK.getStatusCode());
        awaitState(() -> RestAssured.when().get(url.toExternalForm() + "context/transaction-delete1").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testTransactionNewContextPropagation() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction-new").then()
                .statusCode(Response.Status.OK.getStatusCode());
    }

    @Test
    public void testTransactionContextPropagationRsoPublisher() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction-rso-publisher").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        awaitState(() -> RestAssured.when().get(url.toExternalForm() + "context/transaction-delete2").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testTransactionPropagatedToThreadContextCompletionStage() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction-propagated-tc").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        awaitState(() -> RestAssured.when().get(url.toExternalForm() + "context/transaction-delete2").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testTransactionPropagatedToManagedExecutorCompletionStage() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction-propagated-exec").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        awaitState(() -> RestAssured.when().get(url.toExternalForm() + "context/transaction-delete2").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testTransactionPropagatedToPublisher() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction-propagated-publisher").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        awaitState(() -> RestAssured.when().get(url.toExternalForm() + "context/transaction-delete2").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void testTransactionPropagatedToCompletionStageWrappedInPublisher() {
        RestAssured.when().get(url.toExternalForm() + "context/transaction-propagated-cs-wrapped-in-publisher").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .body(equalTo("OK"));
        awaitState(() -> RestAssured.when().get(url.toExternalForm() + "context/transaction-delete3").then()
                .statusCode(Response.Status.OK.getStatusCode()));
    }

    private void awaitState(ThrowingRunnable task) {
        Awaitility.await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(300, TimeUnit.MILLISECONDS)
                .untilAsserted(task);
    }

}
