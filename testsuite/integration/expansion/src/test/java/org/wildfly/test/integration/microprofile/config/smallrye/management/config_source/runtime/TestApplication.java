/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime;

import jakarta.inject.Inject;
import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Test application for runtime config-source removal testing.
 *
 * @author WildFly Team
 */
@ApplicationPath("/runtime-config-test")
public class TestApplication extends Application {

    @Path("/test")
    public static class Resource {

        @Inject
        @ConfigProperty(name = "runtime.test.property", defaultValue = "NOT_FOUND")
        String testProperty;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();
            text.append("runtime.test.property = ").append(testProperty).append("\n");
            return Response.ok(text).build();
        }
    }
}
