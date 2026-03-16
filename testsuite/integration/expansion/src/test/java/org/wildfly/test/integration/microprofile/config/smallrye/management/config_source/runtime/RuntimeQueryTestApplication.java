/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.runtime;

import java.util.Optional;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;

/**
 * JAX-RS application for runtime config-source add testing (WFLY-21615).
 *
 * @author WildFly Team
 */
@ApplicationPath("/runtime-query-test")
public class RuntimeQueryTestApplication extends Application {

    @Path("/test")
    public static class Resource {

        @GET
        @Produces("text/plain")
        public Response doGet(@QueryParam("property") String propertyName) {
            if (propertyName == null || propertyName.isEmpty()) {
                return Response.status(400).entity("Missing 'property' query parameter\n").build();
            }

            // Get Config instance fresh each time to test if it picks up runtime changes
            Config config = ConfigProvider.getConfig();
            Optional<String> value = config.getOptionalValue(propertyName, String.class);

            if (value.isPresent()) {
                String text = propertyName + " = " + value.get() + "\n";
                return Response.ok(text).build();
            } else {
                return Response.status(404).entity("Property not found: " + propertyName + "\n").build();
            }
        }
    }
}
