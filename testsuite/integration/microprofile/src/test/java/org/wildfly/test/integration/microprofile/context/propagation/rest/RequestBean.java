package org.wildfly.test.integration.microprofile.context.propagation.rest;

import javax.enterprise.context.RequestScoped;

@RequestScoped
public class RequestBean {

    public long id() {
        return System.identityHashCode(this);
    }
}
