package org.wildfly.test.integration.microprofile.reactive.context.propagation.custom.context;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.context.ThreadContext;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@Singleton
public class TestBean {
    @Inject
    ThreadContext tc;

}
