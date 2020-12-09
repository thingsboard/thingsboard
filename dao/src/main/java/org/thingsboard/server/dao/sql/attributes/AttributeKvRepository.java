/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.attributes;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;

import java.util.List;
import java.util.UUID;

public interface AttributeKvRepository extends CrudRepository<AttributeKvEntity, AttributeKvCompositeKey> {

    @Query("SELECT a FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType")
    List<AttributeKvEntity> findAllByEntityTypeAndEntityIdAndAttributeType(@Param("entityType") EntityType entityType,
                                                                           @Param("entityId") UUID entityId,
                                                                           @Param("attributeType") String attributeType);

    @Transactional
    @Modifying
    @Query("DELETE FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType " +
            "AND a.id.attributeKey = :attributeKey")
    void delete(@Param("entityType") EntityType entityType,
                @Param("entityId") UUID entityId,
                @Param("attributeType") String attributeType,
                @Param("attributeKey") String attributeKey);

    @Query("SELECT a FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId in (SELECT d.id FROM DeviceEntity d WHERE d.tenantId = :tenantId and d.deviceProfileId = :deviceProfileId)")
    List<AttributeKvEntity> findAllByDeviceProfileId(@Param("tenantId") UUID tenantId,
                                                     @Param("deviceProfileId") UUID deviceProfileId,
                                                     @Param("entityType") EntityType entityType,
                                                     Pageable pageable);

    @Query("SELECT a FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId in (SELECT d.id FROM DeviceEntity d WHERE d.tenantId = :tenantId)")
    List<AttributeKvEntity> findAllByTenantId(@Param("tenantId") UUID tenantId,
                                              @Param("entityType") EntityType entityType,
                                              Pageable pageable);
}

