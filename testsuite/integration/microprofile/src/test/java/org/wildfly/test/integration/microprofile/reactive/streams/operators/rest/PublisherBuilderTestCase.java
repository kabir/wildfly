package org.wildfly.test.integration.microprofile.reactive.streams.operators.rest;

import java.net.URL;

import javax.ws.rs.core.Response;

import org.hamcrest.core.StringContains;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;

import io.restassured.RestAssured;

/**
 * Tests that PublisherBuilder is handled in line with how Publisher is
 * in RestEasy
 *
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
public class PublisherBuilderTestCase {
    @ArquillianResource
    URL url;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "rso-pub-build-endpoint.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .setWebXML(PublisherBuilderTestCase.class.getPackage(), "web.xml")
                .addPackage(PublisherBuilderTestCase.class.getPackage())
                .addClasses(TimeoutUtil.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class);

        return webArchive;
    }

    @Test
    public void testPublisher() {
        RestAssured.when().get(url.toExternalForm() + "pub").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .content(StringContains.containsString("Hi publisher"));
    }

    @Test
    public void testPublisherBuilder() {
        RestAssured.when().get(url.toExternalForm() + "pb").then()
                .statusCode(Response.Status.OK.getStatusCode())
                .content(StringContains.containsString("Hi builder"));
    }

}
