package org.wildfly.test.integration.microprofile.reactive.context.propagation.rest;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public long id() {
        return System.identityHashCode(this);
    }
}
