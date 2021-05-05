/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
import static org.wildfly.extension.microprofile.context.propagation.MicroProfileContextPropagationExtension.CONFIG_CAPABILITY_NAME;
import static org.wildfly.extension.microprofile.context.propagation.MicroProfileContextPropagationExtension.WELD_CAPABILITY_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.RuntimePackageDependency;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.microprofile.context.propagation._private.MicroProfileContextPropagationLogger;
import org.wildfly.extension.microprofile.context.propagation.deployment.ContextPropagationDependencyProcessor;
import org.wildfly.extension.microprofile.context.propagation.deployment.ContextPropagationDeploymentProcessor;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileContextPropagationSubsystemDefinition extends PersistentResourceDefinition {

    private static final String CONTEXT_PROPAGATION_CAPABILITY_NAME = "org.wildfly.microprofile.context-propagation";

    private static final RuntimeCapability<Void> CONTEXT_PROPAGATION_CAPABILITY = RuntimeCapability.Builder
            .of(CONTEXT_PROPAGATION_CAPABILITY_NAME)
            .addRequirements(CONFIG_CAPABILITY_NAME, WELD_CAPABILITY_NAME)
            .build();

    // These all depend on modules which may or may not have been provisioned.
    // For example the JtaContextProvider uses the Jta modules.
    // Set them up as passive so that they are only provisioned if their dependencies
    // have been provisioned
    private static final List<String> PASSIVE_PROVIDER_MODULES =
            Collections.unmodifiableList(Arrays.asList(
                    "io.smallrye.context-propagation.providers.jta",
                    "org.jboss.resteasy.resteasy-context-propagation",
                    "org.wildfly.reactive.context-propagation.provider.plugin.application-naming",
                    "org.wildfly.reactive.context-propagation.provider.servlet"));

    public MicroProfileContextPropagationSubsystemDefinition() {
        super(
                new SimpleResourceDefinition.Parameters(
                        MicroProfileContextPropagationExtension.SUBSYSTEM_PATH,
                        MicroProfileContextPropagationExtension.getResourceDescriptionResolver(MicroProfileContextPropagationExtension.SUBSYSTEM_NAME))
                .setAddHandler(AddHandler.INSTANCE)
                .setRemoveHandler(new ReloadRequiredRemoveStepHandler())
                .setCapabilities(CONTEXT_PROPAGATION_CAPABILITY)
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Collections.emptyList();
    }

    @Override
    public void registerAdditionalRuntimePackages(ManagementResourceRegistration resourceRegistration) {
        List<RuntimePackageDependency> dependencies = new ArrayList<>();
        dependencies.add(RuntimePackageDependency.required("io.smallrye.context-propagation.api"));
        dependencies.add(RuntimePackageDependency.required("io.smallrye.context-propagation"));
        dependencies.add(RuntimePackageDependency.required("io.smallrye.reactive.mutiny.context-propagation"));
        dependencies.add(RuntimePackageDependency.required("javax.enterprise.api"));
        dependencies.add(RuntimePackageDependency.required("org.wildfly.reactive.dep.jts"));
        dependencies.add(RuntimePackageDependency.required("org.wildfly.security.manager"));
        // This is currently only needed by the passive io.smallrye.context-propagation.providers.jta module,
        // but it easier to just provision it here in all cases.
        dependencies.add(RuntimePackageDependency.required("io.smallrye.reactive.converters.api"));

        // Register the modules containing plugins and providers that have dependencies on
        // modules which may or may not be provisioned. Doing these as passive means they
        // will get provisioned if the optional dependencies in their module.xml files are
        // present
        for (String name : PASSIVE_PROVIDER_MODULES) {
            dependencies.add(RuntimePackageDependency.passive(name));
        }

        resourceRegistration.registerAdditionalRuntimePackages(dependencies.toArray(new RuntimePackageDependency[dependencies.size()]));
        resourceRegistration.registerAdditionalRuntimePackages();
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

                    processorTarget.addDeploymentProcessor(
                            MicroProfileContextPropagationExtension.SUBSYSTEM_NAME,
                            DEPENDENCIES,
                            DEPENDENCIES_MICROPROFILE_CONTEXT_PROPAGATION,
                            new ContextPropagationDependencyProcessor(PASSIVE_PROVIDER_MODULES));
                    processorTarget.addDeploymentProcessor(
                            MicroProfileContextPropagationExtension.SUBSYSTEM_NAME,
                            POST_MODULE,
                            POST_MODULE_MICROPROFILE_CONTEXT_PROPAGATION,
                            new ContextPropagationDeploymentProcessor(WELD_CAPABILITY_NAME));
                }
            }, RUNTIME);

            MicroProfileContextPropagationLogger.LOGGER.activatingSubsystem();
        }
    }
}
