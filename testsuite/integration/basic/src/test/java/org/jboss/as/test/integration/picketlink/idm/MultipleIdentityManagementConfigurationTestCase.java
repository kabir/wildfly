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
package org.jboss.as.test.integration.picketlink.idm;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.picketlink.idm.util.AbstractIdentityManagementServerSetupTask;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.config.FileIdentityStoreConfiguration;
import org.picketlink.idm.config.JPAIdentityStoreConfiguration;

import javax.annotation.Resource;

import static org.junit.Assert.assertTrue;

/**
 * @author Pedro Igor
 */
@RunWith(Arquillian.class)
@ServerSetup({
    JPADSBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class,
    FileBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class
})
public class MultipleIdentityManagementConfigurationTestCase {

    @Resource(mappedName = FileBasedPartitionManagerTestCase.PARTITION_MANAGER_JNDI_NAME)
    private PartitionManager filePartitionManager;

    @Resource(mappedName = JPADSBasedPartitionManagerTestCase.PARTITION_MANAGER_JNDI_NAME)
    private PartitionManager jpaDSPartitionManager;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap
                   .create(WebArchive.class, "test.war")
                   .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
                   .addAsManifestResource(new StringAsset("Dependencies: org.picketlink.core meta-inf,org.picketlink.core.api meta-inf,org.picketlink.idm.api meta-inf\n"), "MANIFEST.MF")
                   .addClass(MultipleIdentityManagementConfigurationTestCase.class)
                   .addClass(JPADSBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class)
                   .addClass(FileBasedPartitionManagerTestCase.IdentityManagementServerSetupTask.class)
                   .addClass(AbstractIdentityManagementServerSetupTask.class);
    }

    @Test
    public void testInjection() {
        assertTrue(FileIdentityStoreConfiguration.class.isInstance(this.filePartitionManager.getConfigurations().iterator().next().getStoreConfiguration().get(0)));
        assertTrue(JPAIdentityStoreConfiguration.class.isInstance(this.jpaDSPartitionManager.getConfigurations().iterator().next().getStoreConfiguration().get(0)));
    }

}
