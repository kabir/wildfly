/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.microprofile.context.propagation;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.jboss.as.server.deployment.Phase.POST_MODULE;

import java.util.Collection;
import java.util.Collections;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.context.propagation._private.MicroProfileContextPropagationLogger;
import org.wildfly.extension.microprofile.context.propagation.deployment.ContextPropagationDependencyProcessor;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileContextPropagationSubsystemDefinition extends PersistentResourceDefinition {

    private static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";
    private static final String CONTEXT_PROPAGATION_CAPABILITY_NAME = "org.wildfly.microprofile.context-propagation";

    private static final RuntimeCapability<Void> CONTEXT_PROPAGATION_CAPABILITY = RuntimeCapability.Builder
            .of(CONTEXT_PROPAGATION_CAPABILITY_NAME)
            .addRequirements(WELD_CAPABILITY_NAME)
            .build();

    public MicroProfileContextPropagationSubsystemDefinition() {
        super(
                new SimpleResourceDefinition.Parameters(
                        MicroProfileContextPropagationExtension.SUBSYSTEM_PATH,
                        MicroProfileContextPropagationExtension.getResourceDescriptionResolver(MicroProfileContextPropagationExtension.SUBSYSTEM_NAME))
                .setAddHandler(AddHandler.INSTANCE)
                .setRemoveHandler(new ModelOnlyRemoveStepHandler())
                .setCapabilities(CONTEXT_PROPAGATION_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    static class AddHandler extends AbstractBoottimeAddStepHandler {

        static AddHandler INSTANCE = new AddHandler();

        private AddHandler() {
            super(Collections.emptyList());
        }

        @Override
        protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            super.performBoottime(context, operation, model);

            context.addStep(new AbstractDeploymentChainStep() {
                public void execute(DeploymentProcessorTarget processorTarget) {

                    // TODO Put these into Phase.java
                    final int DEPENDENCIES_MICROPROFILE_CONTEXT_PROPAGATION = 6304;
                    final int POST_MODULE_MICROPROFILE_CONTEXT_PROPAGATION = 14240;

                    processorTarget.addDeploymentProcessor(MicroProfileContextPropagationExtension.SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_CONTEXT_PROPAGATION, new ContextPropagationDependencyProcessor());
                    processorTarget.addDeploymentProcessor(MicroProfileContextPropagationExtension.SUBSYSTEM_NAME, POST_MODULE, POST_MODULE_MICROPROFILE_CONTEXT_PROPAGATION, new ContextPropagationDependencyProcessor());
                }
            }, RUNTIME);

            MicroProfileContextPropagationLogger.LOGGER.activatingSubsystem();
        }
    }
}
