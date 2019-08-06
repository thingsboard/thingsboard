/**
 * Copyright © 2016-2019 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.attributes;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import javax.transaction.Transactional;
import java.util.List;

@SqlDao
public interface AttributeKvRepository extends CrudRepository<AttributeKvEntity, AttributeKvCompositeKey> {

    @Query("SELECT a FROM AttributeKvEntity a WHERE a.id.entityType = :entityType " +
            "AND a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType")
    List<AttributeKvEntity> findAllByEntityTypeAndEntityIdAndAttributeType(@Param("entityType") EntityType entityType,
                                                                           @Param("entityId") String entityId,
                                                                           @Param("attributeType") String attributeType);


    @Transactional
    @Modifying
    @Query(value = "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, long_v, last_update_ts) VALUES (:entityType, :entityId, :attributeType, :attributeKey, :longValue, :lastUpdateTs) ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) DO NOTHING;", nativeQuery = true)
    void saveIfNotExistLong(@Param("entityType") String entityType,
                        @Param("entityId") String entityId,
                        @Param("attributeType") String attributeType,
                        @Param("attributeKey") String attributeKey,
                        @Param("longValue") Long longValue,
                        @Param("lastUpdateTs") Long lastUpdateTs);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, dbl_v, last_update_ts) VALUES (:entityType, :entityId, :attributeType, :attributeKey, :doubleValue, :lastUpdateTs) ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) DO NOTHING;", nativeQuery = true)
    void saveIfNotExistDouble(@Param("entityType") String entityType,
                              @Param("entityId") String entityId,
                              @Param("attributeType") String attributeType,
                              @Param("attributeKey") String attributeKey,
                              @Param("doubleValue") Double doubleValue,
                              @Param("lastUpdateTs") Long lastUpdateTs);

    @Transactional
    @Modifying
    @Query(value = "INSERT INTO attribute_kv (entity_type, entity_id, attribute_type, attribute_key, bool_v, last_update_ts) VALUES (:entityType, :entityId, :attributeType, :attributeKey, :booleanValue, :lastUpdateTs) ON CONFLICT (entity_type, entity_id, attribute_type, attribute_key) DO NOTHING;", nativeQuery = true)
    void saveIfNotExistBool(@Param("entityType") String entityType,
                                @Param("entityId") String entityId,
                                @Param("attributeType") String attributeType,
                                @Param("attributeKey") String attributeKey,
                                @Param("booleanValue") Boolean booleanValue,
                                @Param("lastUpdateTs") Long lastUpdateTs);

}

