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
package org.thingsboard.server.dao.sqlts.ts;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvCompositeKey;
import org.thingsboard.server.dao.model.sqlts.ts.TsKvEntity;

import java.util.List;
import java.util.UUID;

public interface TsKvRepository extends JpaRepository<TsKvEntity, TsKvCompositeKey> {

    /*
     * Using native query to avoid adding 'nulls first' or 'nulls last' (ignoring spring.jpa.properties.hibernate.order_by.default_null_ordering)
     * to the order so that index scan is done instead of full scan.
     *
     * Note: even when setting custom NullHandling for the Sort.Order for non-native queries,
     * it will be ignored and default_null_ordering will be used
     * */
    @Query(value = "SELECT * FROM ts_kv WHERE entity_id = :entityId " +
            "AND key = :entityKey AND ts >= :startTs AND ts < :endTs ", nativeQuery = true)
    List<TsKvEntity> findAllWithLimit(@Param("entityId") UUID entityId,
                                      @Param("entityKey") int key,
                                      @Param("startTs") long startTs,
                                      @Param("endTs") long endTs,
                                      Pageable pageable);

    @Transactional
    @Modifying
    @Query("DELETE FROM TsKvEntity tskv WHERE tskv.entityId = :entityId " +
            "AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    void delete(@Param("entityId") UUID entityId,
                @Param("entityKey") int key,
                @Param("startTs") long startTs,
                @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MAX(tskv.strValue), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.strValue IS NOT NULL " +
            "AND tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findStringMax(@Param("entityId") UUID entityId,
                             @Param("entityKey") int entityKey,
                             @Param("startTs") long startTs,
                             @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MAX(COALESCE(tskv.longValue, -9223372036854775807)), " +
            "MAX(COALESCE(tskv.doubleValue, java.lang.Double.MIN_VALUE)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'MAX', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findNumericMax(@Param("entityId") UUID entityId,
                              @Param("entityKey") int entityKey,
                              @Param("startTs") long startTs,
                              @Param("endTs") long endTs);


    @Query("SELECT new TsKvEntity(MIN(tskv.strValue), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.strValue IS NOT NULL " +
            "AND tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findStringMin(@Param("entityId") UUID entityId,
                             @Param("entityKey") int entityKey,
                             @Param("startTs") long startTs,
                             @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(MIN(COALESCE(tskv.longValue, 9223372036854775807)), " +
            "MIN(COALESCE(tskv.doubleValue, java.lang.Double.MAX_VALUE)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'MIN', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findNumericMin(
            @Param("entityId") UUID entityId,
            @Param("entityKey") int entityKey,
            @Param("startTs") long startTs,
            @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(CASE WHEN tskv.booleanValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.strValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.jsonValue IS NULL THEN 0 ELSE 1 END), MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findCount(@Param("entityId") UUID entityId,
                         @Param("entityKey") int entityKey,
                         @Param("startTs") long startTs,
                         @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(COALESCE(tskv.longValue, 0)), " +
            "SUM(COALESCE(tskv.doubleValue, 0.0)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'AVG', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findAvg(@Param("entityId") UUID entityId,
                       @Param("entityKey") int entityKey,
                       @Param("startTs") long startTs,
                       @Param("endTs") long endTs);

    @Query("SELECT new TsKvEntity(SUM(COALESCE(tskv.longValue, 0)), " +
            "SUM(COALESCE(tskv.doubleValue, 0.0)), " +
            "SUM(CASE WHEN tskv.longValue IS NULL THEN 0 ELSE 1 END), " +
            "SUM(CASE WHEN tskv.doubleValue IS NULL THEN 0 ELSE 1 END), " +
            "'SUM', MAX(tskv.ts)) FROM TsKvEntity tskv " +
            "WHERE tskv.entityId = :entityId AND tskv.key = :entityKey AND tskv.ts >= :startTs AND tskv.ts < :endTs")
    TsKvEntity findSum(@Param("entityId") UUID entityId,
                       @Param("entityKey") int entityKey,
                       @Param("startTs") long startTs,
                       @Param("endTs") long endTs);

}
