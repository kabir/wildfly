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
package org.jboss.as.picketlink.subsystems.idm.service;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.transaction.jta.platform.internal.JBossAppServerJtaPlatform;
import org.jboss.as.picketlink.subsystems.idm.config.JPAStoreSubsystemConfiguration;
import org.jboss.as.picketlink.subsystems.idm.config.JPAStoreSubsystemConfigurationBuilder;
import org.jboss.modules.Module;
import org.picketlink.idm.jpa.internal.JPAIdentityStore;
import org.picketlink.idm.spi.ContextInitializer;
import org.picketlink.idm.spi.IdentityContext;
import org.picketlink.idm.spi.IdentityStore;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.metamodel.EntityType;
import javax.transaction.Status;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.reflect.Modifier.isAbstract;
import static org.jboss.as.picketlink.PicketLinkMessages.MESSAGES;
import static org.picketlink.common.util.StringUtil.isNullOrEmpty;

/**
 * @author Pedro Igor
 */
public class JPAIdentityStoreInitializer implements IdentityStoreInitializer {

    private static final String JPA_ANNOTATION_PACKAGE = "org.picketlink.idm.jpa.annotations";
    private final JPAStoreSubsystemConfigurationBuilder configurationBuilder;
    private final JPAStoreSubsystemConfiguration storeConfig;
    private EntityManagerFactory emf;

    public JPAIdentityStoreInitializer(JPAStoreSubsystemConfigurationBuilder configurationBuilder) {
        this.configurationBuilder = configurationBuilder;
        this.storeConfig = this.configurationBuilder.create();
    }

    @Override
    public void onStart(final PartitionManagerService partitionManagerService) {
        try {
            configureEntityManagerFactory();
            configureEntities();
        } catch (Exception e) {
            throw MESSAGES.idmJpaStartFailed(e);
        }

        configureEntities();

        this.configurationBuilder.addContextInitializer(new ContextInitializer() {
            @Override
            public void initContextForStore(IdentityContext context, IdentityStore<?> store) {
                if (store instanceof JPAIdentityStore) {
                    if (!context.isParameterSet(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER)) {
                        context.setParameter(JPAIdentityStore.INVOCATION_CTX_ENTITY_MANAGER, getEntityManager(partitionManagerService.getTransactionManager().getValue()));
                    }
                }
            }
        });
    }

    @Override
    public void onStop(PartitionManagerService partitionManagerService) {
        if (this.storeConfig.getEntityManagerFactoryJndiName() == null) {
            this.emf.close();
        }
    }

    private void configureEntityManagerFactory() {
        if (this.storeConfig.getEntityManagerFactoryJndiName() != null) {
            this.emf = lookupEntityManagerFactory();
        } else {
            this.emf = createEmbeddedEntityManagerFactory();
        }
    }

    private EntityManagerFactory lookupEntityManagerFactory() {
        try {
            return (EntityManagerFactory) new InitialContext().lookup(this.storeConfig.getEntityManagerFactoryJndiName());
        } catch (NamingException e) {
            throw MESSAGES.idmJpaEMFLookupFailed(this.storeConfig.getEntityManagerFactoryJndiName());
        }
    }

    private EntityManagerFactory createEmbeddedEntityManagerFactory() {
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

        try {
            Map<Object, Object> properties = new HashMap<Object, Object>();
            String dataSourceJndiUrl = this.storeConfig.getDataSourceJndiUrl();

            if (!isNullOrEmpty(dataSourceJndiUrl)) {
                properties.put("javax.persistence.jtaDataSource", dataSourceJndiUrl);
            }

            properties.put(AvailableSettings.JTA_PLATFORM, new JBossAppServerJtaPlatform());

            Module entityModule = this.storeConfig.getEntityModule();

            if (entityModule != null) {
                Thread.currentThread().setContextClassLoader(entityModule.getClassLoader());
            }

            return Persistence.createEntityManagerFactory(this.storeConfig.getEntityModuleUnitName(), properties);
        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }
    }

    private void configureEntities() {
        Set<EntityType<?>> mappedEntities = this.emf.getMetamodel().getEntities();

        for (EntityType<?> entity : mappedEntities) {
            Class<?> javaType = entity.getJavaType();

            if (!isAbstract(javaType.getModifiers()) && isIdentityEntity(javaType)) {
                this.configurationBuilder.mappedEntity(javaType);
            }
        }
    }

    private boolean isIdentityEntity(Class<?> cls) {
        Class<?> checkClass = cls;

        while (!checkClass.equals(Object.class)) {
            for (Annotation a : checkClass.getAnnotations()) {
                if (a.annotationType().getName().startsWith(JPA_ANNOTATION_PACKAGE)) {
                    return true;
                }
            }

            // No class annotation was found, check the fields
            for (Field f : checkClass.getDeclaredFields()) {
                for (Annotation a : f.getAnnotations()) {
                    if (a.annotationType().getName().startsWith(JPA_ANNOTATION_PACKAGE)) {
                        return true;
                    }
                }
            }

            // Check the superclass
            checkClass = checkClass.getSuperclass();
        }

        return false;
    }

    private EntityManager getEntityManager(TransactionManager transactionManager) {
        return (EntityManager) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class<?>[]{EntityManager.class}, new EntityManagerInvocationHandler(this.emf.createEntityManager(), this.storeConfig.getEntityModule(), transactionManager));
    }

    private class EntityManagerInvocationHandler implements InvocationHandler {

        private final EntityManager em;
        private final Module entityModule;
        private final TransactionManager transactionManager;

        public EntityManagerInvocationHandler(EntityManager em, Module entitiesModule, TransactionManager transactionManager) {
            this.em = em;
            this.entityModule = entitiesModule;
            this.transactionManager = transactionManager;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Transaction tx = null;

            if (isTxRequired(method)) {
                if (this.transactionManager.getStatus() == Status.STATUS_NO_TRANSACTION) {
                    this.transactionManager.begin();
                }

                this.em.joinTransaction();

                tx = this.transactionManager.getTransaction();
            }

            ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();

            try {
                if (this.entityModule != null) {
                    Thread.currentThread().setContextClassLoader(this.entityModule.getClassLoader());
                }

                return method.invoke(this.em, args);
            } finally {
                Thread.currentThread().setContextClassLoader(originalClassLoader);

                if (tx != null) {
                    tx.commit();
                    this.transactionManager.suspend();
                }
            }
        }

        private boolean isTxRequired(Method method) {
            String n = method.getName();
            return "flush".equals(n) || "getLockMode".equals(n) || "lock".equals(n) || "merge".equals(n) || "persist".equals(n) || "refresh".equals(n) || "remove".equals(n);
        }
    }
}
