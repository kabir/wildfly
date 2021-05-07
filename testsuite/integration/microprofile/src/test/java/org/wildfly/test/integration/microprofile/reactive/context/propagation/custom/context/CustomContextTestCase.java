package org.wildfly.test.integration.microprofile.reactive.context.propagation.custom.context;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import org.eclipse.microprofile.context.spi.ThreadContextProvider;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.shared.CLIServerSetupTask;
import org.jboss.as.test.shared.TimeoutUtil;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.test.integration.microprofile.reactive.EnableReactiveExtensionsSetupTask;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
@RunWith(Arquillian.class)
@ServerSetup(EnableReactiveExtensionsSetupTask.class)
public class CustomContextTestCase {
    @Inject
    TestBean testBean;

    @Deployment
    public static WebArchive getDeployment() {
        final WebArchive webArchive = ShrinkWrap.create(WebArchive.class, "ctx-ppgn-custom-ctx.war")
                .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                .addPackage(CustomContextTestCase.class.getPackage())
                .addClasses(TimeoutUtil.class, EnableReactiveExtensionsSetupTask.class, CLIServerSetupTask.class)
                .addAsServiceProvider(ThreadContextProvider.class, CustomContextProvider.class);
        return webArchive;
    }

    @Test
    public void testCustomContextPropagation() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // set something to custom context
        CustomContext.set("foo");

        CompletableFuture<String> ret = testBean.tc.withContextCapture(CompletableFuture.completedFuture("void"));
        CompletableFuture<Void> cfs = ret.thenApplyAsync(text -> {
            Assert.assertEquals("foo", CustomContext.get());
            return null;
        }, executor);
        cfs.get();
    }

}
