/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wildfly.test.integration.microprofile.reactive.messaging.tx;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.transaction.Transactional;


@ApplicationScoped
public class TransactionalBean {

    @PersistenceContext(unitName = "test")
    EntityManager em;

    @Transactional
    public void storeValue(String name) {
        ContextEntity entity = new ContextEntity();
        entity.setName(name);
        em.persist(entity);
    }

    @Transactional
    public void checkValues(Set<String> names) {
        checkCount(names.size());

        TypedQuery<ContextEntity> query = em.createQuery("SELECT c from ContextEntity c", ContextEntity.class);
        Set<String> values = query.getResultList().stream().map(v -> v.getName()).collect(Collectors.toSet());
        if (!values.containsAll(names) || !names.containsAll(values)) {
            throw new IllegalStateException("Mismatch of expected names. Expected: " + names + "; actual: " + values);
        }
    }

    @Transactional
    private int checkCount(int expected) {
        TypedQuery<Long> query = em.createQuery("SELECT count(c) from ContextEntity c", Long.class);
        List<Long> result = query.getResultList();
        int count = result.get(0).intValue();
        if (count != expected) {
            throw new IllegalStateException("Expected " + expected + "; got " + count);
        }
        return count;
    }

}
