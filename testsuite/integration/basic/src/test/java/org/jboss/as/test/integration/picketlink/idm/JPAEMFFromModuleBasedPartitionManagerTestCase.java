package org.jboss.as.test.integration.picketlink.idm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.junit.InSequence;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.arquillian.container.ManagementClient;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.picketlink.idm.entities.AbstractCredentialTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.AccountEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.AttributeTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.AttributedTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.GroupTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.IdentityTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.PartitionTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.PasswordCredentialTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.RelationshipIdentityTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.RelationshipIdentityTypeReferenceEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.RelationshipTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.entities.RoleTypeEntity;
import org.jboss.as.test.integration.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.IdentityManager;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.RelationshipManager;
import org.picketlink.idm.credential.Credentials;
import org.picketlink.idm.credential.Password;
import org.picketlink.idm.credential.UsernamePasswordCredentials;
import org.picketlink.idm.model.Attribute;
import org.picketlink.idm.model.basic.BasicModel;
import org.picketlink.idm.model.basic.Realm;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE_ENTITY_MODULE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.JPA_STORE_ENTITY_MODULE_UNIT_NAME;
import static org.jboss.as.test.integration.picketlink.idm.util.ModuleUtils.createTestModule;
import static org.jboss.as.test.integration.picketlink.idm.util.ModuleUtils.deleteRecursively;
import static org.jboss.as.test.integration.picketlink.idm.util.ModuleUtils.getModulePath;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.picketlink.idm.model.basic.BasicModel.getRole;
import static org.picketlink.idm.model.basic.BasicModel.getUser;
import static org.picketlink.idm.model.basic.BasicModel.hasRole;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup(JPAEMFFromModuleBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class)
public class JPAEMFFromModuleBasedPartitionManagerTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/JPAEMFBasedPartitionManager";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
            .create(WebArchive.class, "test.war")
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
            .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.idm.api meta-inf\n"), "MANIFEST.MF")
            .addClass(JPAEMFFromModuleBasedPartitionManagerTestCase.class)
            .addClass(AbstractIdentityManagementServerSetupTask.class)
            .addClass(AbstractBasicIdentityManagementTestCase.class);
    }

    @ArquillianResource
    private InitialContext initialContext;

    private PartitionManager partitionManager;

    @Test
    @InSequence(1)
    public void testPartitionManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        Realm partition = partitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM);

        if (partition != null) {
            partitionManager.remove(partition);
        }

        partitionManager.add(new Realm(Realm.DEFAULT_REALM));

        assertNotNull(partitionManager.getPartition(Realm.class, Realm.DEFAULT_REALM));
    }

    @Test
    @InSequence(2)
    public void testUserManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        User user = new User("johny");

        identityManager.add(user);

        identityManager.lookupIdentityById(User.class, user.getId());

        assertNotNull(getUser(identityManager, user.getLoginName()));
    }

    @Test
    @InSequence(3)
    public void testCredentialManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        User user = getUser(identityManager, "johny");
        Password password = new Password("abcd1234");

        identityManager.updateCredential(user, password);

        UsernamePasswordCredentials credentials = new UsernamePasswordCredentials(user.getLoginName(), password);

        identityManager.validateCredentials(credentials);

        assertEquals(Credentials.Status.VALID, credentials.getStatus());
    }

    @Test
    @InSequence(4)
    public void testRoleManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        Role role = new Role("admin");

        identityManager.add(role);

        assertNotNull(getRole(identityManager, role.getName()));
    }

    @Test
    @InSequence(5)
    public void testRelationshipManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        User user = getUser(identityManager, "johny");
        Role role = getRole(identityManager, "admin");

        RelationshipManager relationshipManager = partitionManager.createRelationshipManager();

        BasicModel.grantRole(relationshipManager, user, role);

        assertTrue(hasRole(relationshipManager, user, role));
    }

    @Test
    @InSequence(6)
    public void testAttributeManagement() throws Exception {
        PartitionManager partitionManager = getPartitionManager();
        IdentityManager identityManager = partitionManager.createIdentityManager();
        User user = getUser(identityManager, "johny");

        assertNull(user.getAttribute("testAttribute"));

        user.setAttribute(new Attribute<String>("testAttribute", "value"));

        identityManager.update(user);

        assertNotNull(user.getAttribute("testAttribute"));
        assertEquals("value", user.getAttribute("testAttribute").getValue());
    }

    protected PartitionManager getPartitionManager() {
        if (this.partitionManager == null) {
            try {
                this.partitionManager = (PartitionManager) this.initialContext.lookup("java:/" + PARTITION_MANAGER_JNDI_NAME);
            } catch (NamingException e) {
                fail(e.getMessage());
            }
        }

        return this.partitionManager;
    }

    static class IdentityManagementServerSetupTask extends AbstractIdentityManagementServerSetupTask {

        public IdentityManagementServerSetupTask() {
            super("jpa.emf.idm", PARTITION_MANAGER_JNDI_NAME);
        }

        @Override
        public void setup(ManagementClient managementClient, String containerId) throws Exception {
            deleteRecursively(new File(getModulePath(), "test/picketlink-emf-module-test"));
            createTestModule("picketlink-emf-module-test", "JPAEMFFromModuleBasedPartitionManagerTestCase-module.xml", new File(JPAEMFFromModuleBasedPartitionManagerTestCase.class
                .getResource(JPAEMFFromModuleBasedPartitionManagerTestCase.class.getSimpleName() + "-persistence.xml")
                .getFile()), JPAEMFFromModuleBasedPartitionManagerTestCase.class, AbstractCredentialTypeEntity.class, AttributedTypeEntity.class, AttributeTypeEntity.class, GroupTypeEntity.class, IdentityTypeEntity.class, PartitionTypeEntity.class, PasswordCredentialTypeEntity.class, RelationshipIdentityTypeEntity.class, RelationshipIdentityTypeReferenceEntity.class, RelationshipTypeEntity.class, RoleTypeEntity.class, AccountEntity.class);
            super.setup(managementClient, containerId);
        }

        @Override
        public void tearDown(ManagementClient managementClient, String containerId) throws Exception {
            super.tearDown(managementClient, containerId);
            deleteRecursively(new File(getModulePath(), "picketlink-test"));
        }

        @Override
        protected void doCreateIdentityManagement(ModelNode identityManagementAddOperation, ModelNode operationSteps) {
            ModelNode operationAddIdentityConfiguration = Util
                .createAddOperation(createIdentityConfigurationPathAddress("jpa.emf.store"));

            operationSteps.add(operationAddIdentityConfiguration);

            ModelNode operationAddIdentityStore = createIdentityStoreAddOperation(operationAddIdentityConfiguration);

            operationSteps.add(operationAddIdentityStore);

            ModelNode operationAddSupportedTypes = createSupportedAllTypesAddOperation(operationAddIdentityStore);

            operationSteps.add(operationAddSupportedTypes);
        }

        private ModelNode createIdentityStoreAddOperation(ModelNode identityConfigurationModelNode) {
            PathAddress pathAddress = PathAddress.pathAddress(identityConfigurationModelNode.get(OP_ADDR)).append(JPA_STORE
                .getName(), JPA_STORE.getName());
            ModelNode operationAddIdentityStore = Util.createAddOperation(pathAddress);

            operationAddIdentityStore.get(JPA_STORE_ENTITY_MODULE.getName()).set("test.picketlink-emf-module-test");
            operationAddIdentityStore.get(JPA_STORE_ENTITY_MODULE_UNIT_NAME.getName()).set("user-defined-pu");

            return operationAddIdentityStore;
        }
    }
}
