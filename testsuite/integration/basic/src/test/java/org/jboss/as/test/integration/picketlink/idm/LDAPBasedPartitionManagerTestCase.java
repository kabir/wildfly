package org.jboss.as.test.integration.picketlink.idm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.picketlink.subsystems.idm.model.ModelElement;
import org.jboss.as.test.integration.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;
import org.jboss.as.test.integration.picketlink.idm.util.LdapMapping;
import org.jboss.as.test.integration.picketlink.idm.util.LdapServerSetupTask;
import org.jboss.dmr.ModelNode;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.IdentityType;
import org.picketlink.idm.model.Relationship;
import org.picketlink.idm.model.basic.Agent;
import org.picketlink.idm.model.basic.Grant;
import org.picketlink.idm.model.basic.Group;
import org.picketlink.idm.model.basic.Role;
import org.picketlink.idm.model.basic.User;

import javax.annotation.Resource;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.COMMON_CLASS;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.COMMON_SUPPORTS_ALL;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.LDAP_STORE;
import static org.jboss.as.picketlink.subsystems.idm.model.ModelElement.SUPPORTED_TYPE;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup({LdapServerSetupTask.class, LDAPBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class
})
public class LDAPBasedPartitionManagerTestCase extends AbstractBasicIdentityManagementTestCase {

    static final String PARTITION_MANAGER_JNDI_NAME = "picketlink/LDAPBasedPartitionManager";

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
               .create(WebArchive.class, "test.war")
               .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
               .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.core meta-inf,org.picketlink.core.api meta-inf,org.picketlink.idm.api meta-inf\n"), "MANIFEST.MF")
               .addClass(LDAPBasedPartitionManagerTestCase.class)
               .addClass(LdapServerSetupTask.class)
               .addClass(AbstractIdentityManagementServerSetupTask.class)
               .addClass(AbstractBasicIdentityManagementTestCase.class);
    }

    @Resource(mappedName = PARTITION_MANAGER_JNDI_NAME)
    private PartitionManager partitionManager;

    @Override
    @Test
    public void testPartitionManagement() throws Exception {
        // ldap store does not support partition management
    }

    @Override
    protected PartitionManager getPartitionManager() {
        return this.partitionManager;
    }

    static class IdentityManagementServerSetupTask extends AbstractIdentityManagementServerSetupTask {

        public IdentityManagementServerSetupTask() {
            super("ldap.idm", PARTITION_MANAGER_JNDI_NAME);
        }

        @Override
        protected void doCreateIdentityManagement(ModelNode identityManagementAddOperation, ModelNode operationSteps) {
            ModelNode operationAddIdentityConfiguration = Util.createAddOperation(createIdentityConfigurationPathAddress("ldap.store"));

            operationSteps.add(operationAddIdentityConfiguration);

            ModelNode operationAddIdentityStore = createIdentityStoreAddOperation(operationAddIdentityConfiguration);

            operationSteps.add(operationAddIdentityStore);

            createSupportedTypesAddOperation(operationSteps, operationAddIdentityStore);

            LdapMapping agentMapping = new LdapMapping(Agent.class.getName(), "ou=Agent,dc=jboss,dc=org", "account");

            agentMapping.addAttribute("loginName", "uid", true, false);
            agentMapping.addAttribute("createdDate", "createTimeStamp", false, true);

            operationSteps.add(agentMapping.createAddOperation(operationAddIdentityStore));

            LdapMapping userMapping = new LdapMapping(User.class.getName(), "ou=People,dc=jboss,dc=org", "inetOrgPerson, organizationalPerson");

            userMapping.addAttribute("loginName", "uid", true, false);
            userMapping.addAttribute("firstName", "cn", false, false);
            userMapping.addAttribute("lastName", "sn", false, false);
            userMapping.addAttribute("email", "mail", false, false);
            userMapping.addAttribute("createdDate", "createTimeStamp", false, true);

            operationSteps.add(userMapping.createAddOperation(operationAddIdentityStore));

            LdapMapping roleMapping = new LdapMapping(Role.class.getName(), "ou=Roles,dc=jboss,dc=org", "groupOfNames");

            roleMapping.addAttribute("name", "cn", true, false);
            roleMapping.addAttribute("createdDate", "createTimeStamp", false, true);

            operationSteps.add(roleMapping.createAddOperation(operationAddIdentityStore));

            LdapMapping groupMapping = new LdapMapping(Group.class.getName(), "ou=Groups,dc=jboss,dc=org", "groupOfNames");

            groupMapping.addAttribute("name", "cn", true, false);
            groupMapping.addAttribute("createdDate", "createTimeStamp", false, true);

            operationSteps.add(groupMapping.createAddOperation(operationAddIdentityStore));

            LdapMapping grantMapping = new LdapMapping(Grant.class.getName(), Role.class.getName());

            grantMapping.addAttribute("assignee", "member", true, true);

            operationSteps.add(grantMapping.createAddOperation(operationAddIdentityStore));
        }

        private void createSupportedTypesAddOperation(ModelNode operationSteps, ModelNode operationAddIdentityStore) {
            ModelNode operationAddSupportedTypes = createSupportedAllTypesAddOperation(operationAddIdentityStore);

            operationAddSupportedTypes.get(COMMON_SUPPORTS_ALL.getName()).set(false);

            operationSteps.add(operationAddSupportedTypes);

            ModelNode identityTypeAddOperation = Util.createAddOperation(PathAddress.pathAddress(operationAddSupportedTypes.get(OP_ADDR))
                                                             .append(SUPPORTED_TYPE.getName(), IdentityType.class.getName()));

            identityTypeAddOperation.get(COMMON_CLASS.getName()).set(IdentityType.class.getName());

            operationSteps.add(identityTypeAddOperation);

            ModelNode relationshipAddOperation = Util.createAddOperation(PathAddress.pathAddress(operationAddSupportedTypes.get(OP_ADDR))
                                                             .append(SUPPORTED_TYPE.getName(), Relationship.class.getName()));

            relationshipAddOperation.get(COMMON_CLASS.getName()).set(Relationship.class.getName());

            operationSteps.add(relationshipAddOperation);
        }

        private ModelNode createIdentityStoreAddOperation(ModelNode identityConfigurationModelNode) {
            PathAddress pathAddress = PathAddress.pathAddress(identityConfigurationModelNode.get(OP_ADDR))
                                      .append(LDAP_STORE.getName(), LDAP_STORE.getName());
            ModelNode identityStore = Util.createAddOperation(pathAddress);

            identityStore.get(ModelElement.COMMON_URL.getName()).set("ldap://localhost:10389");
            identityStore.get(ModelElement.LDAP_STORE_BIND_DN.getName()).set("uid=admin,ou=system");
            identityStore.get(ModelElement.LDAP_STORE_BIND_CREDENTIAL.getName()).set("secret");
            identityStore.get(ModelElement.LDAP_STORE_BASE_DN_SUFFIX.getName()).set("dc=jboss,dc=org");

            return identityStore;
        }
    }
}
