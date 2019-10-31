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

package org.wildfly.extension.microprofile.reactive.messaging;

import static org.jboss.as.controller.OperationContext.Stage.RUNTIME;
import static org.jboss.as.server.deployment.Phase.DEPENDENCIES;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.reactive.messaging.MicroProfileReactiveMessagingExtension.SUBSYSTEM_PATH;

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
import org.wildfly.extension.microprofile.reactive.messaging._private.MicroProfileReactiveMessagingLogger;
import org.wildfly.extension.microprofile.reactive.messaging.deployment.ReactiveMessagingDependencyProcessor;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileReactiveMessagingSubsystemDefinition extends PersistentResourceDefinition {

    private static final String REACTIVE_MESSAGING_CAPABILITY_NAME = "org.wildfly.microprofile.reactive-messaging";

    private static final String REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME = "org.wildfly.microprofile.reactive-streams-operators";

    private static final RuntimeCapability<Void> REACTIVE_STREAMS_OPERATORS_CAPABILITY = RuntimeCapability.Builder
            .of(REACTIVE_MESSAGING_CAPABILITY_NAME)
            .addRequirements(MicroProfileReactiveMessagingExtension.WELD_CAPABILITY_NAME)
            .addRequirements(REACTIVE_STREAMS_OPERATORS_CAPABILITY_NAME)
            .addRequirements()
            .build();

    public MicroProfileReactiveMessagingSubsystemDefinition() {
        super(
                new SimpleResourceDefinition.Parameters(
                        SUBSYSTEM_PATH,
                        MicroProfileReactiveMessagingExtension.getResourceDescriptionResolver(SUBSYSTEM_NAME))
                .setAddHandler(AddHandler.INSTANCE)
                .setRemoveHandler(new ModelOnlyRemoveStepHandler())
                .setCapabilities(REACTIVE_STREAMS_OPERATORS_CAPABILITY)
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
                    final int DEPENDENCIES_MICROPROFILE_REACTIVE_MESSAGING = 6288;

                    processorTarget.addDeploymentProcessor(SUBSYSTEM_NAME, DEPENDENCIES, DEPENDENCIES_MICROPROFILE_REACTIVE_MESSAGING, new ReactiveMessagingDependencyProcessor());
                }
            }, RUNTIME);

            MicroProfileReactiveMessagingLogger.LOGGER.activatingSubsystem();
        }
    }
}
