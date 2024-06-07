/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.ObjectDao;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.thingsboard.server.common.data.ObjectType.ATTRIBUTE_KV;
import static org.thingsboard.server.common.data.ObjectType.AUDIT_LOG;
import static org.thingsboard.server.common.data.ObjectType.EVENT;
import static org.thingsboard.server.common.data.ObjectType.LATEST_TS_KV;
import static org.thingsboard.server.common.data.ObjectType.RELATION;
import static org.thingsboard.server.common.data.ObjectType.TENANT;
import static org.thingsboard.server.common.data.ObjectType.TS_KV;

@Slf4j
@DaoSqlTest
public class EntityDaoRegistryTest extends AbstractServiceTest {

    @Autowired
    EntityDaoRegistry entityDaoRegistry;

    @Autowired
    List<JpaRepository<?, ?>> repositories;

    @Test
    public void givenAllEntityTypes_whenGetDao_thenAllPresent() {
        for (EntityType entityType : EntityType.values()) {
            Dao<?> dao = assertDoesNotThrow(() -> entityDaoRegistry.getDao(entityType));
            assertThat(dao).isNotNull();
        }
    }

    @Test
    public void givenAllDaos_whenFindById_thenOk() {
        for (EntityType entityType : EntityType.values()) {
            Dao<?> dao = entityDaoRegistry.getDao(entityType);
            assertDoesNotThrow(() -> {
                dao.findById(TenantId.SYS_TENANT_ID, UUID.randomUUID());
            });
        }
    }

    @Test
    public void givenAllDaos_whenFindIdsByTenantIdAndIdOffset_thenOk() {
        for (EntityType entityType : EntityType.values()) {
            Dao<?> dao = entityDaoRegistry.getDao(entityType);
            try {
                dao.findIdsByTenantIdAndIdOffset(TenantId.SYS_TENANT_ID, null, 10);
                dao.findIdsByTenantIdAndIdOffset(TenantId.SYS_TENANT_ID, UUID.randomUUID(), 10);
            } catch (Exception e) {
                String error = ExceptionUtils.getRootCauseMessage(e);
                if (error.contains("tenant_id")) {
                    log.debug("[{}] Ignoring not found tenant_id column", entityType);
                } else {
                    fail("findIdsByTenantIdAndIdOffset for " + entityType + " dao threw error: " + error);
                }
            }
        }
    }

    /*
     * Verifying that all the repositories are successfully bootstrapped, when using Lazy Jpa bootstrap mode
     * */
    @Test
    public void testJpaRepositories() {
        for (var repository : repositories) {
            repository.count();
        }
    }

    @Test
    public void givenAllTenantEntityDaos_whenFindAllByTenantId_thenOk() {
        Set<ObjectType> ignored = EnumSet.of(TENANT, RELATION, EVENT, ATTRIBUTE_KV, LATEST_TS_KV, TS_KV, AUDIT_LOG);
        for (ObjectType type : ObjectType.values()) {
            if (ignored.contains(type)) {
                continue;
            }

            TenantEntityDao<?> dao = assertDoesNotThrow(() -> entityDaoRegistry.getTenantEntityDao(type));
            assertDoesNotThrow(() -> {
                dao.findAllByTenantId(tenantId, new PageLink(100));
            });
        }
    }

    @Test
    public void givenAllObjectTypes_whenGetDao_thenPresent() {
        for (ObjectType type : ObjectType.values()) {
            ObjectDao<?> dao = assertDoesNotThrow(() -> entityDaoRegistry.getDao(type));
            assertThat(dao).isNotNull();
        }
    }

}
