/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.eclipse.microprofile.config.ConfigProvider;
import io.smallrye.config.SmallRyeConfig;

/**
 * A registry whose items can be iterated over.
 * @author Paul Ferraro
 */
public class IterableRegistry<T> implements Iterable<T>, Registry<T> {

    // Use a sorted map since we need a deterministic sort order. The order things are added to the SmallRyeConfigBuilder
    // has influence on the final sorting order, and we have tests checking this order is deterministic.
    private final Map<String, T> objects = new ConcurrentSkipListMap<>();

    @Override
    public void register(String name, T object) {
        this.objects.put(name, object);
        refreshConfigCache();
    }

    @Override
    public void unregister(String name) {
        this.objects.remove(name);
        refreshConfigCache();
    }

    @Override
    public Iterator<T> iterator() {
        return this.objects.values().iterator();
    }

    private void refreshConfigCache() {
        try {
            org.eclipse.microprofile.config.Config config = ConfigProvider.getConfig();
            if (config instanceof SmallRyeConfig) {
                // Refresh the cached property names so our new entries show up elsewhere
                ((SmallRyeConfig) config).getLatestPropertyNames();
            }
        } catch (IllegalStateException e) {
            // Config not available yet (during boot) - that's fine
        }
    }
}
