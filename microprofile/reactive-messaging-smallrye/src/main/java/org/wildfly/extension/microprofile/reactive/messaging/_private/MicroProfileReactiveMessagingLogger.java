/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.microprofile.reactive.messaging._private;

import static org.jboss.logging.Logger.Level.INFO;

import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * Log messages for WildFly microprofile-health-smallrye Extension.
 *
 * @author <a href="kkhan@redhat.com">Kabir Khan</a> (c) 2019 Red Hat inc.
 */
@MessageLogger(projectCode = "WFLYRXSTOPS", length = 4)
public interface MicroProfileReactiveMessagingLogger extends BasicLogger {

    MicroProfileReactiveMessagingLogger LOGGER = Logger.getMessageLogger(MicroProfileReactiveMessagingLogger.class, "org.wildfly.extension.microprofile.reactive-streams-operators-smallrye");

    /**
     * Logs an informational message indicating the subsystem is being activated.
     */
    @LogMessage(level = INFO)
    @Message(id = 1, value = "Activating Eclipse MicroProfile Reactive Messaging Subsystem")
    void activatingSubsystem();


    @Message(id = 2, value = "Deployment %s requires use of the '%s' capability but it is not currently registered")
    DeploymentUnitProcessingException deploymentRequiresCapability(String deploymentName, String capabilityName);

    @LogMessage(level = INFO)
    @Message(id = 3, value = "Intermediate module %s is not present. Skipping recursively adding modules from it")
    void intermediateModuleNotPresent(String intermediateModuleName);
}
