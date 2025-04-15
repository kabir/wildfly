/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.config.smallrye._private;

import java.lang.invoke.MethodHandles;

import org.jboss.logging.BasicLogger;
import org.jboss.logging.Logger;
import org.jboss.logging.annotations.MessageLogger;

/**
 * @author <a href="http://jmesnil.net/">Jeff Mesnil</a> (c) 2017 Red Hat inc.
 */
@MessageLogger(projectCode = "WFLYCONFAPI", length = 4)
public interface MicroProfileConfigApiLogger extends BasicLogger {

    /**
     * The root logger with a category of the package name.
     */
    MicroProfileConfigApiLogger ROOT_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), MicroProfileConfigApiLogger.class,"org.wildfly.extension.microprofile.config.smallrye");

}
