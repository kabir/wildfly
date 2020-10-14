/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.test.integration.microprofile.reactive.streams.operators;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.eclipse.microprofile.reactive.streams.operators.spi.ReactiveStreamsEngine;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
public class ReactiveStreamsOperatorsInjectedReactiveEngineProviderSanityTestCase {

    @Inject
    ReactiveStreamsEngine engine;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class,  "rx-stream-ops.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addClass(ReactiveStreamsOperatorsInjectedReactiveEngineProviderSanityTestCase.class);
        return webArchive;
    }

    @Test
    public void testReactiveApiWithInjectedEngine() throws Exception {
        Assert.assertNotNull(engine);

        CompletionStage<List<String>> cs = ReactiveStreams.of("this", "is", "only", "a", "test")
                .map(String::toUpperCase) // Transform the words
                .filter(s -> s.length() > 3) // Filter items
                .collect(Collectors.toList())
                .run(engine);

        List<String> result = cs.toCompletableFuture().get();

        Assert.assertEquals(3, result.size());
        Assert.assertEquals("THIS", result.get(0));
        Assert.assertEquals("ONLY", result.get(1));
        Assert.assertEquals("TEST", result.get(2));
    }
}
