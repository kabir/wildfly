package org.wildfly.microprofile.reactive.messaging.kafka.config;

import java.util.Collections;
import java.util.Map;

import org.eclipse.microprofile.config.spi.ConfigSource;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class DisableKafkaConnectorTracingConfigSource implements ConfigSource {

    private Map<String, String> PROPERTIES = Collections.singletonMap("mp.messaging.connector.smallrye-kafka.tracing-enabled", "false");

    @Override
    public Map<String, String> getProperties() {
        return PROPERTIES;
    }

    @Override
    public String getValue(String propertyName) {
        return PROPERTIES.get(propertyName);
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrdinal() {
        // Make the ordinal high so it cannot be overridden
        return Integer.MAX_VALUE;
    }
}
