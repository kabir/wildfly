/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye.deployment;

import static org.jboss.as.weld.Capabilities.WELD_CAPABILITY_NAME;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.AttachmentKey;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.weld.WeldCapability;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.wildfly.extension.microprofile.config.smallrye._private.MicroProfileConfigLogger;
import org.wildfly.extension.microprofile.config.smallrye.cdi.MicroprofileConfigEarClassLoaderCdiExtension;

/**
 */
public class SubsystemDeploymentProcessor implements DeploymentUnitProcessor {

    public static final AttachmentKey<Config> CONFIG = AttachmentKey.create(Config.class);

    public SubsystemDeploymentProcessor() {
    }

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        Config config = ConfigProviderResolver.instance().getConfig(module.getClassLoader());
        deploymentUnit.putAttachment(CONFIG, config);

        handleEars(deploymentUnit, module.getClassLoader());
    }

    private void handleEars(DeploymentUnit deploymentUnit, ModuleClassLoader classLoader) {
        if (!isEar(deploymentUnit)) {
            return;
        }

        try {
            final CapabilityServiceSupport support = deploymentUnit.getAttachment(Attachments.CAPABILITY_SERVICE_SUPPORT);
            final WeldCapability weldCapability = support.getCapabilityRuntimeAPI(WELD_CAPABILITY_NAME, WeldCapability.class);
            if (weldCapability == null || !weldCapability.isPartOfWeldDeployment(deploymentUnit)) {
                MicroProfileConfigLogger.ROOT_LOGGER.debug("The deployment does not have Jakarta Contexts and Dependency Injection enabled. " +
                        "Skipping MicroProfile Telemetry integration.");
            } else {


                weldCapability.registerExtensionInstance(new MicroprofileConfigEarClassLoaderCdiExtension(classLoader),
                        deploymentUnit);
            }
        } catch (CapabilityServiceSupport.NoSuchCapabilityException e) {
            throw new IllegalStateException("No capability");
//            throw MPTEL_LOGGER.deploymentRequiresCapability(deploymentPhaseContext.getDeploymentUnit().getName(),
//                    WELD_CAPABILITY_NAME);
        }

    }

    private boolean isEar(DeploymentUnit deploymentUnit) {
        try {
            if (DeploymentTypeMarker.isType(DeploymentType.EAR, deploymentUnit)) {
                return true;
            }
        } catch (Exception e) {
            // The ee subsystem is not available so we don't handle ears
        }
        return false;
    }

    @Override
    public void undeploy(DeploymentUnit context) {
        Config config = context.removeAttachment(CONFIG);

        ConfigProviderResolver.instance().releaseConfig(config);
    }
}
