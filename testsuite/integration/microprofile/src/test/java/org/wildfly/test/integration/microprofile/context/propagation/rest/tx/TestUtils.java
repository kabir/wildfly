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

package org.wildfly.test.integration.microprofile.context.propagation.rest.tx;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class TestUtils {
    static int count(EntityManager em) {
        TypedQuery<Long> query = em.createQuery("SELECT count(c) from ContextEntity c", Long.class);
        List<Long> result = query.getResultList();
        return result.get(0).intValue();
    }

    static int deleteAll(EntityManager em) {
        Query query = em.createQuery("DELETE from ContextEntity");
        return query.executeUpdate();
    }

    static void assertSame(Object expected, Object actual) {
        if (expected != actual) {
            throw new IllegalStateException(expected + " is not the same as " + actual);
        }
    }

    static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new IllegalStateException("Expected " + expected + "; got " + actual);
        }
    }

    static void assertEquals(Object expected, Object actual) {
        if (expected == null && actual != null || !expected.equals(actual)) {
            throw new IllegalStateException("Expected " + expected + "; got " + actual);
        }
    }

    static void assertNotNull(String msg, Object o) {
        if (o == null) {
            throw new IllegalStateException(msg);
        }
    }
}
