package org.wildfly.test.integration.microprofile.reactive.context.propagation.sanity.rest;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public long id() {
        return System.identityHashCode(this);
    }
}
