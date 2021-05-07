package org.wildfly.test.integration.microprofile.reactive.context.propagation.custom.context;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class CustomContext {

    public static final String NAME = "MyContext";
    private static ThreadLocal<String> context = ThreadLocal.withInitial(() -> "");

    private CustomContext() {
    }

    public static String get() {
        return context.get();
    }

    public static void set(String label) {
        context.set(label);
    }
}
