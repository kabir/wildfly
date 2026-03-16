/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.reload;

import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;

@Path("/custom-config")
public class TestResource {

    @Inject
    Config config;

    @GET
    @Path("/test")
    @Produces("text/plain")
    public Response test(@QueryParam("property") String propertyName) {
        if (propertyName == null || propertyName.isEmpty()) {
            return Response.status(400).entity("Missing 'property' query parameter\n").build();
        }

        Optional<String> value = config.getOptionalValue(propertyName, String.class);

        if (value.isPresent()) {
            String text = propertyName + " = " + value.get() + "\n";
            return Response.ok(text).build();
        } else {
            return Response.status(404).entity("Property not found: " + propertyName + "\n").build();
        }
    }
}
