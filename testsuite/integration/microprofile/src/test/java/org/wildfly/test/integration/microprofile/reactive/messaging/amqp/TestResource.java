/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.amqp;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/last")
public class TestResource {

    @Inject
    ConsumingBean bean;

    @GET
    public long getLast() {
        return bean.get();
    }

    @Inject
    EmitterBean emitterBean;

    @POST
    @Path("{value}")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response send(@PathParam("value") String value) {
        emitterBean.send(value);
        return Response.ok().build();
    }

    @GET
    @Path("/emitter")
    @Produces(APPLICATION_JSON)
    public List<String> getReceivedFromEmitter() {
        return emitterBean.getReceived();
    }


}
