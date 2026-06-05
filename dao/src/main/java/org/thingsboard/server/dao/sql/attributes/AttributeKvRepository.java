/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.BooleanDataEntry;
import org.thingsboard.server.common.data.kv.DoubleDataEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.data.kv.LongDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.model.sql.AttributeKvCompositeKey;
import org.thingsboard.server.dao.model.sql.AttributeKvEntity;

import java.util.List;
import java.util.UUID;

public interface AttributeKvRepository extends JpaRepository<AttributeKvEntity, AttributeKvCompositeKey> {

    @Query("SELECT a FROM AttributeKvEntity a WHERE a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType")
    List<AttributeKvEntity> findAllByEntityIdAndAttributeType(@Param("entityId") UUID entityId,
                                                              @Param("attributeType") int attributeType);

    @Transactional
    @Modifying
    @Query("DELETE FROM AttributeKvEntity a WHERE a.id.entityId = :entityId " +
            "AND a.id.attributeType = :attributeType " +
            "AND a.id.attributeKey = :attributeKey")
    void delete(@Param("entityId") UUID entityId,
                @Param("attributeType") int attributeType,
                @Param("attributeKey") int attributeKey);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE " +
            "entity_id in (SELECT id FROM device WHERE tenant_id = :tenantId and device_profile_id = :deviceProfileId limit 100) ORDER BY attribute_key", nativeQuery = true)
    List<Integer> findAllKeysByDeviceProfileId(@Param("tenantId") UUID tenantId,
                                               @Param("deviceProfileId") UUID deviceProfileId);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE " +
            "entity_id in (SELECT id FROM device WHERE tenant_id = :tenantId limit 100) ORDER BY attribute_key", nativeQuery = true)
    List<Integer> findAllKeysByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE " +
            "entity_id in :entityIds ORDER BY attribute_key", nativeQuery = true)
    List<Integer> findAllKeysByEntityIds(@Param("entityIds") List<UUID> entityIds);

    @Query(value = "SELECT DISTINCT attribute_key FROM attribute_kv WHERE " +
            "entity_id in :entityIds AND attribute_type = :attributeType ORDER BY attribute_key", nativeQuery = true)
    List<Integer> findAllKeysByEntityIdsAndAttributeType(@Param("entityIds") List<UUID> entityIds,
                                                         @Param("attributeType") int attributeType);

    @Query(value = """
            SELECT DISTINCT ON (a.attribute_key)
                kd.key AS strKey,
                a.bool_v AS boolV, a.str_v AS strV, a.long_v AS longV,
                a.dbl_v AS dblV, a.json_v AS jsonV,
                a.last_update_ts AS lastUpdateTs, a.version AS version
            FROM attribute_kv a
            INNER JOIN key_dictionary kd ON a.attribute_key = kd.key_id
            WHERE a.entity_id IN :entityIds AND a.attribute_type = :attributeType
            ORDER BY a.attribute_key, a.last_update_ts DESC""", nativeQuery = true)
    List<AttributeKvProjection> findLatestByEntityIdsAndAttributeType(@Param("entityIds") List<UUID> entityIds,
                                                                      @Param("attributeType") int attributeType);

    @Query(value = "SELECT attribute_key, attribute_type, entity_id, bool_v, dbl_v, json_v, last_update_ts, long_v, str_v, version FROM attribute_kv WHERE (entity_id, attribute_type, attribute_key) > " +
            "(:entityId, :attributeType, :attributeKey) ORDER BY entity_id, attribute_type, attribute_key LIMIT :batchSize", nativeQuery = true)
    List<AttributeKvEntity> findNextBatch(@Param("entityId") UUID entityId,
                                          @Param("attributeType") int attributeType,
                                          @Param("attributeKey") int attributeKey,
                                          @Param("batchSize") int batchSize);

    interface AttributeKvProjection {

        String getStrKey();

        Boolean getBoolV();

        String getStrV();

        Long getLongV();

        Double getDblV();

        String getJsonV();

        Long getLastUpdateTs();

        Long getVersion();

        static AttributeKvEntry toAttributeKvEntry(AttributeKvProjection p) {
            KvEntry kvEntry = null;
            if (p.getStrV() != null) {
                kvEntry = new StringDataEntry(p.getStrKey(), p.getStrV());
            } else if (p.getBoolV() != null) {
                kvEntry = new BooleanDataEntry(p.getStrKey(), p.getBoolV());
            } else if (p.getDblV() != null) {
                kvEntry = new DoubleDataEntry(p.getStrKey(), p.getDblV());
            } else if (p.getLongV() != null) {
                kvEntry = new LongDataEntry(p.getStrKey(), p.getLongV());
            } else if (p.getJsonV() != null) {
                kvEntry = new JsonDataEntry(p.getStrKey(), p.getJsonV());
            }
            return new BaseAttributeKvEntry(kvEntry, p.getLastUpdateTs(), p.getVersion());
        }

    }

}
