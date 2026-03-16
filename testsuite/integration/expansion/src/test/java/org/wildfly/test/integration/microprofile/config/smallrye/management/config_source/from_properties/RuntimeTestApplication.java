/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_properties;

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
 * JAX-RS application for runtime config-source testing.
 * This application has no injected properties, so it can deploy before
 * config-sources are added at runtime.
 */
@ApplicationPath("/custom-config-source")
public class RuntimeTestApplication extends Application {

    /**
     * Dynamic query resource for runtime config testing.
     * Uses ConfigProvider.getConfig() to query properties dynamically.
     */
    @Path("/query")
    public static class DynamicQueryResource {

        @GET
        @Produces("text/plain")
        public Response doGet(@QueryParam("property") String propertyName) {
            if (propertyName == null || propertyName.isEmpty()) {
                return Response.status(400).entity("Missing 'property' query parameter\n").build();
            }

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
