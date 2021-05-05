package org.wildfly.test.integration.microprofile.reactive.streams.operators.rest;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.microprofile.reactive.streams.operators.PublisherBuilder;
import org.eclipse.microprofile.reactive.streams.operators.ReactiveStreams;
import org.reactivestreams.Publisher;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Path("/")
@Produces(MediaType.TEXT_PLAIN)
public class RsoEndpoint {
    ManagedExecutor unchangedExecutor = ManagedExecutor.builder().build();

    @GET
    @Path("/pub")
    public Publisher<String> testPublisher() {
        return createPublisherBuilder("Hi publisher").buildRs();
    }

    @GET
    @Path("/pb")
    public PublisherBuilder<String> testPublisherBuilder() {
        return createPublisherBuilder("Hi builder");
    }

    private PublisherBuilder<String> createPublisherBuilder(String msg) {
        CompletionStage<String> cs = unchangedExecutor.supplyAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return msg;
        });
        return ReactiveStreams.fromCompletionStage(cs);
    }
}
