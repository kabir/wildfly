/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.reload;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;

import org.eclipse.microprofile.config.Config;

@Path("/custom-config")
public class TestResource {

    @Inject
    Config config;

    @GET
    @Path("/test")
    @Produces("text/plain")
    public String test() {
        StringBuilder builder = new StringBuilder();
        config.getPropertyNames().forEach(propertyName -> {
            String propertyValue = config.getConfigValue(propertyName).getValue();
            builder.append(propertyName).append("=").append(propertyValue).append("\n");
        });
        return builder.toString();
    }
}
