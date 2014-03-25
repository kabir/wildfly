/*
 * Copyright (C) 2014 Red Hat, inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.jboss.as.controller.test;

import java.util.Locale;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import org.jboss.as.controller.ControlledProcessState;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceBuilder;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_REQUIRES_RELOAD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROCESS_STATE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESPONSE_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESTART_REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Test;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2013 Red Hat, inc.
 */
public class WriteAttributeOperationTestCase extends AbstractControllerTestBase {

    private static final OperationDefinition SETUP_OP_DEF = new SimpleOperationDefinitionBuilder("setup", new NonResolvingResourceDescriptionResolver())
            .setPrivateEntry()
            .build();

    protected static final SimpleAttributeDefinition TIMEOUT = new SimpleAttributeDefinitionBuilder("timeout", ModelType.LONG)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    protected static final SimpleAttributeDefinition CLASSIC = new SimpleAttributeDefinitionBuilder("classic", ModelType.BOOLEAN)
            .setAllowNull(true)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setDefaultValue(new ModelNode(true))
            .setAllowExpression(true)
            .setMaxSize(1)
            .build();

    private static final OperationStepHandler handler = new ReloadRequiredWriteAttributeHandler(CLASSIC, TIMEOUT);

    @Override
    protected void initModel(Resource rootResource, ManagementResourceRegistration rootRegistration) {
        GlobalOperationHandlers.registerGlobalOperations(rootRegistration, processType);
        rootRegistration.registerOperationHandler(SETUP_OP_DEF, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final ModelNode model = new ModelNode();
                //Atttributes
                model.get("profile", "profileA", "subsystem", "subsystem1", "classic").set(true);
                model.get("profile", "profileA", "subsystem", "subsystem1", "timeout").set(1000L);
                createModel(context, model);
                context.stepCompleted();
            }
        }
        );
        ResourceDefinition profileDef = ResourceBuilder.Factory.create(PathElement.pathElement("profile", "*"),
                new NonResolvingResourceDescriptionResolver())
                .addReadOnlyAttribute(SimpleAttributeDefinitionBuilder.create("name", ModelType.STRING, false).setMinSize(1).build())
                .build();

        ManagementResourceRegistration profileReg = rootRegistration.registerSubModel(profileDef);

        ManagementResourceRegistration profileSub1Reg = profileReg.registerSubModel(PathElement.pathElement("subsystem", "subsystem1"), new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                ModelNode node = new ModelNode();
                node.get(DESCRIPTION).set("A test subsystem 1");
                node.get(ATTRIBUTES, "classic", TYPE).set(ModelType.BOOLEAN);
                node.get(ATTRIBUTES, "classic", DESCRIPTION).set("Is classic");
                node.get(ATTRIBUTES, "classic", REQUIRED).set(false);
                node.get(ATTRIBUTES, "timeout", RESTART_REQUIRED).set("all-services");
                node.get(ATTRIBUTES, "timeout", TYPE).set(ModelType.LONG);
                node.get(ATTRIBUTES, "timeout", DESCRIPTION).set("A r/o long");
                node.get(ATTRIBUTES, "timeout", REQUIRED).set(true);
                node.get(ATTRIBUTES, "timeout", RESTART_REQUIRED).set("all-services");
                return node;
            }
        });
        profileSub1Reg.registerReadWriteAttribute(CLASSIC, null, handler);
        profileSub1Reg.registerReadWriteAttribute(TIMEOUT, null, handler);
    }

    @Test
    public void testWriteReloadAttribute() throws Exception {

        //Just make sure it works as expected for an existant resource
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set("classic");
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.BOOLEAN));
        assertThat(result.asBoolean(), is(true));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set("timeout");
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.LONG));
        assertThat(result.asLong(), is(1000L));

        ModelNode write = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        write.get(NAME).set("classic");
        write.get(VALUE).set(false);
        result = executeCheckNoFailure(write);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).asBoolean(), is(true));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).asString(), is(ControlledProcessState.State.RELOAD_REQUIRED.toString()));

        write = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        write.get(NAME).set("timeout");
        write.get(VALUE).set(10000L);
        result = executeCheckNoFailure(write);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).asBoolean(), is(true));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).asString(), is(ControlledProcessState.State.RELOAD_REQUIRED.toString()));
    }

    @Test
    public void testWriteReloadAttributeWithoutChange() throws Exception {

        //Just make sure it works as expected for an existant resource
        ModelNode operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set("classic");
        ModelNode result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.BOOLEAN));
        assertThat(result.asBoolean(), is(true));

        operation = createOperation(READ_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        operation.get(NAME).set("timeout");
        result = executeForResult(operation);
        assertThat(result, is(notNullValue()));
        assertThat(result.getType(), is(ModelType.LONG));
        assertThat(result.asLong(), is(1000L));

        ModelNode rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        rewrite.get(NAME).set("classic");
        rewrite.get(VALUE).set(true);
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(),  is(false));

        rewrite = createOperation(WRITE_ATTRIBUTE_OPERATION, "profile", "profileA", "subsystem", "subsystem1");
        rewrite.get(NAME).set("timeout");
        rewrite.get(VALUE).set(1000L);
        result = executeCheckNoFailure(rewrite);
        assertThat(result, is(notNullValue()));
        assertThat(result.get(RESPONSE_HEADERS, OPERATION_REQUIRES_RELOAD).isDefined(), is(false));
        assertThat(result.get(RESPONSE_HEADERS, PROCESS_STATE).isDefined(),  is(false));
    }
}
