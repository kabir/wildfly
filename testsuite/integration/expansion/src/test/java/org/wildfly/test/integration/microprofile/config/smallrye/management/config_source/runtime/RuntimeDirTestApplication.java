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
 * Test application for runtime directory-based config-source testing.
 *
 * @author Claude Code (Phase 1 test for WFLY-21615)
 */
@ApplicationPath("/runtime-dir-config-test")
public class RuntimeDirTestApplication extends Application {

    static final String PROP_FROM_DIR_A = "prop-from-dir-a";
    static final String PROP_FROM_DIR_B = "prop-from-dir-b";

    @Path("/test")
    public static class Resource {

        @Inject
        @ConfigProperty(name = PROP_FROM_DIR_A)
        String propA;

        @Inject
        @ConfigProperty(name = PROP_FROM_DIR_B)
        String propB;

        @GET
        @Produces("text/plain")
        public Response doGet() {
            StringBuilder text = new StringBuilder();
            text.append(PROP_FROM_DIR_A + " = " + propA + "\n");
            text.append(PROP_FROM_DIR_B + " = " + propB + "\n");
            return Response.ok(text).build();
        }
    }
}
