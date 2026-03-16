/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime_ordinal;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * REST application to test runtime ordinal changes.
 *
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@ApplicationPath("/custom-config-source")
public class TestApplication extends Application {
    static final String PRIORITY_TEST = "priority-test";

    @Path("/test")
    public static class Resource {

        @Inject
        @ConfigProperty(name = PRIORITY_TEST)
        String priorityTest;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();
            text.append(PRIORITY_TEST + " = " + priorityTest + "\n");
            return Response.ok(text).build();
        }
    }
}
