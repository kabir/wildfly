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

package org.wildfly.test.integration.mp.tck.reactive.messaging;

import javax.enterprise.inject.spi.DeploymentException;

import org.jboss.arquillian.container.spi.client.container.DeploymentExceptionTransformer;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class WildFlyDeploymentExceptionTransformer implements DeploymentExceptionTransformer {
    @Override
    public Throwable transform(Throwable throwable) {
        if (throwable instanceof org.jboss.arquillian.container.spi.client.container.DeploymentException) {
            // This ends up running on the client, and the arq DeploymentException does not actually
            // have the cause populated. However, the message contains a summary of what happened on the server,
            // and will look something like:
            //
            // Cannot deploy test.war: {"WFLYCTL0062: Composite operation failed and was rolled back. Steps that failed:" => {"Operation step-1" => {"WFLYCTL0080: Failed services" => {"jboss.deployment.unit.\"test.war\".WeldStartService" => "Failed to start service
            //     Caused by: org.jboss.weld.exceptions.DeploymentException: Unknown connector for dummy-source-2.
            //     Caused by: java.lang.IllegalArgumentException: Unknown connector for dummy-source-2."}}}}
            //
            // So we parse the string looking for the relevant parts here. The Weld DeploymentException extends
            // the javax.enterprise.inject.spi.DeploymentException wanted by the test, so it has happened if
            // we can find it in the exception message.
            String msg = throwable.getMessage();
            if (msg.contains("WFLYCTL0080") && msg.contains("org.jboss.weld.exceptions.DeploymentException")) {
                return new DeploymentException(msg);
            }
        }
        return null;
    }
}
