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
import org.wildfly.test.integration.microprofile.config.smallrye.management.config_source.from_class.CustomConfigSource;

/**
 * Test application for runtime class-based config-source testing.
 *
 * @author Claude Code (Phase 1 test for WFLY-21615)
 */
@ApplicationPath("/runtime-config-test")
public class RuntimeTestApplication extends Application {

    @Path("/test")
    public static class Resource {

        @Inject
        @ConfigProperty(name = CustomConfigSource.PROP_NAME)
        String prop;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();
            text.append(CustomConfigSource.PROP_NAME + " = " + prop + "\n");
            return Response.ok(text).build();
        }
    }
}
