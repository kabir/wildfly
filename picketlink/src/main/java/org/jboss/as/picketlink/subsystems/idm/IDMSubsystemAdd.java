package org.jboss.as.picketlink.subsystems.idm;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.picketlink.PicketLinkLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import java.util.List;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IDMSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final IDMSubsystemAdd INSTANCE = new IDMSubsystemAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }

    @Override
    public void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                       ServiceVerificationHandler verificationHandler, List<ServiceController<?>> controllers) throws OperationFailedException {
        PicketLinkLogger.ROOT_LOGGER.activatingSubsystem();
    }
}
