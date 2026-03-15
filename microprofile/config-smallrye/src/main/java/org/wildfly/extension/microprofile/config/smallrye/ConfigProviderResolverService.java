/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Service that provides the ConfigProviderResolver capability.
 * This allows dependent subsystems (like OpenAPI) to create service dependencies
 * ensuring config-sources are registered before they call ConfigProvider.getConfig().
 *
 * @author Claude (AI Assistant)
 */
public class ConfigProviderResolverService implements Service {
    private final ConfigProviderResolver resolver;

    ConfigProviderResolverService(ConfigProviderResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void start(StartContext context) {
        // Service is now available for dependent subsystems to use
    }

    @Override
    public void stop(StopContext context) {
        // Cleanup if needed
    }

    public ConfigProviderResolver getValue() {
        return this.resolver;
    }
}
