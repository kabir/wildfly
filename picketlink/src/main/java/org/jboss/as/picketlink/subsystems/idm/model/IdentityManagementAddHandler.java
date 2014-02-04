/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.picketlink.subsystems.idm.model;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
import org.jboss.as.naming.ValueManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.picketlink.subsystems.idm.config.JPAStoreSubsystemConfiguration;
import org.jboss.as.picketlink.subsystems.idm.config.JPAStoreSubsystemConfigurationBuilder;
import org.jboss.as.picketlink.subsystems.idm.service.FileIdentityStoreInitializer;
import org.jboss.as.picketlink.subsystems.idm.service.JPAIdentityStoreInitializer;
import org.jboss.as.picketlink.subsystems.idm.service.PartitionManagerService;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.InjectedValue;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.FileStoreConfigurationBuilder;
import org.picketlink.idm.config.IdentityConfigurationBuilder;
import org.picketlink.idm.config.IdentityStoreConfigurationBuilder;
import org.picketlink.idm.config.LDAPMappingConfigurationBuilder;
import org.picketlink.idm.config.LDAPStoreConfigurationBuilder;
import org.picketlink.idm.config.NamedIdentityConfigurationBuilder;
import org.picketlink.idm.model.AttributedType;
import org.picketlink.idm.model.Relationship;

import javax.transaction.TransactionManager;
import java.util.List;

import static org.jboss.as.controller.PathAddress.EMPTY_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.picketlink.PicketLinkMessages.MESSAGES;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.FILE_STORE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.IDENTITY_CONFIGURATION;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.IDENTITY_STORE_CREDENTIAL_HANDLER;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.LDAP_STORE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.LDAP_STORE_ATTRIBUTE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.LDAP_STORE_MAPPING;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.SUPPORTED_TYPE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.SUPPORTED_TYPES;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IdentityManagementAddHandler extends AbstractAddStepHandler {

    static final IdentityManagementAddHandler INSTANCE = new IdentityManagementAddHandler();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (SimpleAttributeDefinition attribute : IdentityManagementResourceDefinition.INSTANCE.getAttributes()) {
            attribute.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String federationName = address.getLastElement().getValue();
        createPartitionManagerService(context, federationName, model, verificationHandler, newControllers);
    }

    public void createPartitionManagerService(final OperationContext context, String federationName, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        String jndiName = IdentityManagementResourceDefinition.IDENTITY_MANAGEMENT_JNDI_URL.resolveModelAttribute(context, model).asString();
        IdentityConfigurationBuilder builder = new IdentityConfigurationBuilder();
        PartitionManagerService partitionManagerService = new PartitionManagerService(federationName, jndiName, builder);
        ServiceBuilder<PartitionManager> serviceBuilder = context.getServiceTarget().addService(PartitionManagerService.createServiceName(federationName), partitionManagerService);
        ModelNode identityManagement = Resource.Tools.readModel(context.readResource(EMPTY_ADDRESS));

        for (Property identityConfiguration : identityManagement.get(IDENTITY_CONFIGURATION.getName()).asPropertyList()) {
            String configurationName = identityConfiguration.getName();
            NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder = builder.named(configurationName);

            for (ModelNode store : identityConfiguration.getValue().asList()) {
                configureIdentityStore(context, serviceBuilder, partitionManagerService, namedIdentityConfigurationBuilder, store);
            }
        }

        ServiceController<PartitionManager> controller = serviceBuilder.addListener(verificationHandler).setInitialMode(Mode.PASSIVE).install();

        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    private void configureIdentityStore(OperationContext context, ServiceBuilder<PartitionManager> serviceBuilder, PartitionManagerService partitionManagerService, NamedIdentityConfigurationBuilder namedIdentityConfigurationBuilder, ModelNode modelNode) throws OperationFailedException {
        Property prop = modelNode.asProperty();
        String storeType = prop.getName();
        ModelNode identityStore = prop.getValue().asProperty().getValue();
        IdentityStoreConfigurationBuilder storeConfig = null;

        if (storeType.equals(JPA_STORE.getName())) {
            storeConfig = configureJPAIdentityStore(context, serviceBuilder, partitionManagerService, identityStore, namedIdentityConfigurationBuilder);
        } else if (storeType.equals(FILE_STORE.getName())) {
            storeConfig = configureFileIdentityStore(context, serviceBuilder, partitionManagerService, identityStore, namedIdentityConfigurationBuilder);
        } else if (storeType.equals(LDAP_STORE.getName())) {
            storeConfig = configureLDAPIdentityStore(context, identityStore, namedIdentityConfigurationBuilder);
        }

        ModelNode supportAttributeNode = JPAStoreResourceDefinition.SUPPORT_ATTRIBUTE.resolveModelAttribute(context, identityStore);

        storeConfig.supportAttributes(supportAttributeNode.asBoolean());

        ModelNode supportCredentialNode = JPAStoreResourceDefinition.SUPPORT_CREDENTIAL.resolveModelAttribute(context, identityStore);

        storeConfig.supportCredentials(supportCredentialNode.asBoolean());

        configureSupportedTypes(context, identityStore, storeConfig);
        configureCredentialHandlers(context, identityStore, storeConfig);
    }

    private LDAPStoreConfigurationBuilder configureLDAPIdentityStore(OperationContext context, ModelNode ldapIdentityStore, NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        LDAPStoreConfigurationBuilder storeConfig = builder.stores().ldap();
        ModelNode url = LDAPStoreResourceDefinition.URL.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode bindDn = LDAPStoreResourceDefinition.BIND_DN.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode bindCredential = LDAPStoreResourceDefinition.BIND_CREDENTIAL.resolveModelAttribute(context, ldapIdentityStore);
        ModelNode baseDn = LDAPStoreResourceDefinition.BASE_DN_SUFFIX.resolveModelAttribute(context, ldapIdentityStore);

        if (url.isDefined()) {
            storeConfig.url(url.asString());
        }

        if (bindDn.isDefined()) {
            storeConfig.bindDN(bindDn.asString());
        }

        if (bindCredential.isDefined()) {
            storeConfig.bindCredential(bindCredential.asString());
        }

        if (baseDn.isDefined()) {
            storeConfig.baseDN(baseDn.asString());
        }

        if (ldapIdentityStore.hasDefined(LDAP_STORE_MAPPING.getName())) {
            for (ModelNode mappingNode : ldapIdentityStore.get(LDAP_STORE_MAPPING.getName()).asList()) {
                ModelNode ldapMapping = mappingNode.asProperty().getValue();
                String mappingClass = LDAPStoreMappingResourceDefinition.CLASS_NAME.resolveModelAttribute(context, ldapMapping).asString();
                ModelNode moduleNode = LDAPStoreMappingResourceDefinition.MODULE.resolveModelAttribute(context, ldapMapping);
                LDAPMappingConfigurationBuilder storeMapping = storeConfig.mapping(this.<AttributedType>loadClass(moduleNode, mappingClass));

                ModelNode relatesTo = LDAPStoreMappingResourceDefinition.RELATES_TO.resolveModelAttribute(context, ldapMapping);

                if (relatesTo.isDefined()) {
                    storeMapping.forMapping(this.<AttributedType>loadClass(moduleNode, relatesTo.asString()));
                } else {
                    String baseDN = LDAPStoreMappingResourceDefinition.BASE_DN.resolveModelAttribute(context, ldapMapping).asString();

                    storeMapping.baseDN(baseDN);

                    String objectClasses = LDAPStoreMappingResourceDefinition.OBJECT_CLASSES.resolveModelAttribute(context, ldapMapping).asString();

                    for (String objClass : objectClasses.split(",")) {
                        if (!objClass.trim().isEmpty()) {
                            storeMapping.objectClasses(objClass);
                        }
                    }

                    ModelNode parentAttributeName = LDAPStoreMappingResourceDefinition.PARENT_ATTRIBUTE.resolveModelAttribute(context, ldapMapping);

                    if (parentAttributeName.isDefined()) {
                        storeMapping.parentMembershipAttributeName(parentAttributeName.asString());
                    }
                }

                if (ldapMapping.hasDefined(LDAP_STORE_ATTRIBUTE.getName())) {
                    for (ModelNode attributeNode : ldapMapping.get(LDAP_STORE_ATTRIBUTE.getName()).asList()) {
                        ModelNode attribute = attributeNode.asProperty().getValue();
                        String name = LDAPStoreAttributeResourceDefinition.NAME.resolveModelAttribute(context, attribute).asString();
                        String ldapName = LDAPStoreAttributeResourceDefinition.LDAP_NAME.resolveModelAttribute(context, attribute).asString();
                        boolean readOnly = LDAPStoreAttributeResourceDefinition.READ_ONLY.resolveModelAttribute(context, attribute).asBoolean();

                        if (readOnly) {
                            storeMapping.readOnlyAttribute(name, ldapName);
                        } else {
                            boolean isIdentifier = LDAPStoreAttributeResourceDefinition.IS_IDENTIFIER.resolveModelAttribute(context, attribute).asBoolean();
                            storeMapping.attribute(name, ldapName, isIdentifier);
                        }
                    }
                }
            }
        }

        return storeConfig;
    }

    private IdentityStoreConfigurationBuilder configureFileIdentityStore(OperationContext context, ServiceBuilder<PartitionManager> serviceBuilder, PartitionManagerService partitionManagerService, ModelNode resource, final NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        FileStoreConfigurationBuilder fileStoreBuilder = builder.stores().file();
        String workingDir = FileStoreResourceDefinition.WORKING_DIR.resolveModelAttribute(context, resource).asString();
        String relativeTo = FileStoreResourceDefinition.RELATIVE_TO.resolveModelAttribute(context, resource).asString();
        ModelNode alwaysCreateFiles = FileStoreResourceDefinition.ALWAYS_CREATE_FILE.resolveModelAttribute(context, resource);
        ModelNode asyncWrite = FileStoreResourceDefinition.ASYNC_WRITE.resolveModelAttribute(context, resource);
        ModelNode asyncWriteThreadPool = FileStoreResourceDefinition.ASYNC_WRITE_THREAD_POOL.resolveModelAttribute(context, resource);

        fileStoreBuilder.preserveState(!alwaysCreateFiles.asBoolean());
        fileStoreBuilder.asyncWrite(asyncWrite.asBoolean());
        fileStoreBuilder.asyncWriteThreadPool(asyncWriteThreadPool.asInt());

        serviceBuilder.addDependency(PathManagerService.SERVICE_NAME, PathManager.class, partitionManagerService.getPathManager());

        partitionManagerService.register(new FileIdentityStoreInitializer(fileStoreBuilder, workingDir, relativeTo));

        return fileStoreBuilder;
    }

    private JPAStoreSubsystemConfigurationBuilder configureJPAIdentityStore(OperationContext context, ServiceBuilder<PartitionManager> serviceBuilder, PartitionManagerService partitionManagerService, final ModelNode identityStore, final NamedIdentityConfigurationBuilder builder) throws OperationFailedException {
        JPAStoreSubsystemConfigurationBuilder storeConfig = builder.stores().add(JPAStoreSubsystemConfiguration.class, JPAStoreSubsystemConfigurationBuilder.class);

        ModelNode jpaDataSourceNode = JPAStoreResourceDefinition.DATA_SOURCE.resolveModelAttribute(context, identityStore);
        ModelNode jpaEntityModule = JPAStoreResourceDefinition.ENTITY_MODULE.resolveModelAttribute(context, identityStore);
        ModelNode jpaEntityModuleUnitName = JPAStoreResourceDefinition.ENTITY_MODULE_UNIT_NAME.resolveModelAttribute(context, identityStore);
        ModelNode jpaEntityManagerFactoryNode = JPAStoreResourceDefinition.ENTITY_MANAGER_FACTORY.resolveModelAttribute(context, identityStore);

        if (jpaEntityModule.isDefined()) {
            storeConfig.entityModule(jpaEntityModule.asString());
        }

        storeConfig.entityModuleUnitName(jpaEntityModuleUnitName.asString());

        if (jpaDataSourceNode.isDefined()) {
            storeConfig.dataSourceJndiUrl(toJndiName(jpaDataSourceNode.asString()));
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(toJndiName(jpaDataSourceNode.asString()).split("/")));
        }

        if (jpaEntityManagerFactoryNode.isDefined()) {
            storeConfig.entityManagerFactoryJndiName(jpaEntityManagerFactoryNode.asString());
            serviceBuilder.addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jpaEntityManagerFactoryNode.asString().split("/")),
                                            ValueManagedReferenceFactory.class, new InjectedValue<ValueManagedReferenceFactory>());
        }

        serviceBuilder.addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, partitionManagerService.getTransactionManager());

        partitionManagerService.register(new JPAIdentityStoreInitializer(storeConfig));

        return storeConfig;
    }

    @SuppressWarnings("unchecked")
    private void configureSupportedTypes(OperationContext context, ModelNode identityStore, IdentityStoreConfigurationBuilder storeConfig) throws OperationFailedException {
        if (identityStore.hasDefined(SUPPORTED_TYPES.getName())) {
            ModelNode featuresSet = identityStore.get(SUPPORTED_TYPES.getName()).asProperty().getValue();
            ModelNode supportsAll = SupportedTypesResourceDefinition.SUPPORTS_ALL.resolveModelAttribute(context, featuresSet);

            if (supportsAll.asBoolean()) {
                storeConfig.supportAllFeatures();
            }

            if (featuresSet.hasDefined(SUPPORTED_TYPE.getName())) {
                for (Property featureNode : featuresSet.get(SUPPORTED_TYPE.getName()).asPropertyList()) {
                    ModelNode feature = featureNode.getValue();
                    String typeName = SupportedTypeResourceDefinition.CLASS_NAME.resolveModelAttribute(context, feature).asString();
                    ModelNode moduleNode = SupportedTypeResourceDefinition.MODULE.resolveModelAttribute(context, feature);
                    Class<? extends AttributedType> attributedTypeClass = loadClass(moduleNode, typeName);

                    if (Relationship.class.isAssignableFrom(attributedTypeClass)) {
                        storeConfig.supportGlobalRelationship((Class<? extends Relationship>) attributedTypeClass);
                    } else {
                        storeConfig.supportType(attributedTypeClass);
                    }
                }
            }
        }
    }

    private void configureCredentialHandlers(OperationContext context, ModelNode identityStore, IdentityStoreConfigurationBuilder storeConfig) throws OperationFailedException {
        if (identityStore.hasDefined(IDENTITY_STORE_CREDENTIAL_HANDLER.getName())) {
            for (ModelNode credentialHandler : identityStore.get(IDENTITY_STORE_CREDENTIAL_HANDLER.getName()).asList()) {
                String typeName = CredentialHandlerResourceDefinition.CLASS_NAME.resolveModelAttribute(context, credentialHandler.asProperty().getValue()).asString();
                ModelNode moduleNode = CredentialHandlerResourceDefinition.MODULE.resolveModelAttribute(context, credentialHandler.asProperty().getValue());

                storeConfig.addCredentialHandler(loadClass(moduleNode, typeName));
            }
        }
    }

    private String toJndiName(String jndiName) {
        if (jndiName != null) {
            if (jndiName.startsWith("java:")) {
                return jndiName.substring(jndiName.indexOf(":") + 1);
            }
        }

        return jndiName;
    }

    private Module getModule(ModelNode moduleNode) {
        Module module;

        if (moduleNode.isDefined()) {
            ModuleLoader moduleLoader = Module.getBootModuleLoader();
            try {
                module = moduleLoader.loadModule(ModuleIdentifier.create(moduleNode.asString()));
            } catch (ModuleLoadException e) {
                throw MESSAGES.moduleCouldNotLoad(moduleNode.asString(), e);
            }
        } else {
            // fallback to caller module.
            module = Module.getCallerModule();
        }

        return module;
    }

    @SuppressWarnings("unchecked")
    private <T> Class<T> loadClass(ModelNode moduleNode, String typeName) {
        try {
            return (Class<T>) getModule(moduleNode).getClassLoader().loadClass(typeName);
        } catch (ClassNotFoundException cnfe) {
            throw MESSAGES.couldNotLoadClass(typeName, cnfe);
        }
    }

}
