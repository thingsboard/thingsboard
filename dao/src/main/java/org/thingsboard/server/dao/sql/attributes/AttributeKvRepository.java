// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.attributes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
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

    @Query(value = "SELECT attribute_key, attribute_type, entity_id, bool_v, dbl_v, json_v, last_update_ts, long_v, str_v, version FROM attribute_kv WHERE (entity_id, attribute_type, attribute_key) > " +
            "(:entityId, :attributeType, :attributeKey) ORDER BY entity_id, attribute_type, attribute_key LIMIT :batchSize", nativeQuery = true)
    List<AttributeKvEntity> findNextBatch(@Param("entityId") UUID entityId,
                                          @Param("attributeType") int attributeType,
                                          @Param("attributeKey") int attributeKey,
                                          @Param("batchSize") int batchSize);

}
