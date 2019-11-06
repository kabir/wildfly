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

package org.wildfly.extension.microprofile.reactive.messaging.deployment;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.DependencySpec;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleDependencySpec;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.wildfly.extension.microprofile.reactive.messaging._private.MicroProfileReactiveMessagingLogger;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ReactiveMessagingDependencyProcessor implements DeploymentUnitProcessor {

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        addModuleDependencies(deploymentUnit);
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }

    private void addModuleDependencies(DeploymentUnit deploymentUnit) {
        final ModuleSpecification moduleSpecification = deploymentUnit.getAttachment(Attachments.MODULE_SPECIFICATION);
        final ModuleLoader moduleLoader = Module.getBootModuleLoader();

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.reactive-messaging.api", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.reactive.messaging", false, false, true, false));

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.config", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.config.api", false, false, true, false));

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.reactive-streams-operators.core", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.eclipse.microprofile.reactive-streams-operators.api", false, false, true, false));

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "org.reactivestreams", false, false, true, false));
        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.reactive.streams-operators", false, false, true, false));

        // These are optional über modules that export all the independent connectors/clients. However, it seems
        // to confuse the ExternalBeanArchiveProcessor which provides the modules to scan for beans, so we
        // load them and list them all individually instead
        //moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.smallrye.reactive.reactive-messaging.connector", true, false, true, false));
        addDependenciesForIntermediateModule(moduleSpecification, moduleLoader, "io.smallrye.reactive.messaging.connector");

        moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, "io.vertx.client", true, false, true, false));
    }

    private void addDependenciesForIntermediateModule(ModuleSpecification moduleSpecification, ModuleLoader moduleLoader, String intermediateModuleName) {
        try {
            Module module = moduleLoader.loadModule(intermediateModuleName);
            for (DependencySpec dep : module.getDependencies()) {
                if (dep instanceof ModuleDependencySpec) {
                    ModuleDependencySpec mds = (ModuleDependencySpec) dep;
                    moduleSpecification.addSystemDependency(new ModuleDependency(moduleLoader, mds.getName(), mds.isOptional(), false, true, false));
                }
            }
        } catch (ModuleLoadException e) {
            // The module was not provisioned
            MicroProfileReactiveMessagingLogger.LOGGER.intermediateModuleNotPresent(intermediateModuleName);
        }
    }
}
