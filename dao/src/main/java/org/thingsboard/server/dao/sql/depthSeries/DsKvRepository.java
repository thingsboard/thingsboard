/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.depthSeries;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.scheduling.annotation.Async;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.DsKvCompositeKey;
import org.thingsboard.server.dao.model.sql.DsKvEntity;
import org.thingsboard.server.dao.model.sql.TsKvCompositeKey;
import org.thingsboard.server.dao.model.sql.DsKvEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SqlDao
public interface DsKvRepository extends CrudRepository<DsKvEntity, DsKvCompositeKey> {

    @Query("SELECT dskv FROM DsKvEntity dskv WHERE dskv.entityId = :entityId " +
            "AND dskv.entityType = :entityType AND dskv.key = :entityKey " +
            "AND dskv.ds > :startDs AND dskv.ds < :endDs ORDER BY dskv.ds DESC")
    List<DsKvEntity> findAllWithLimit(@Param("entityId") String entityId,
                                      @Param("entityType") EntityType entityType,
                                      @Param("entityKey") String key,
                                      @Param("startDs") Double startDs,
                                      @Param("endDs") Double endDs,
                                      Pageable pageable);

    @Async
    @Query("SELECT new DsKvEntity(MAX(dskv.strValue), MAX(dskv.longValue), MAX(dskv.doubleValue)) FROM DsKvEntity dskv " +
            "WHERE dskv.entityId = :entityId AND dskv.entityType = :entityType " +
            "AND dskv.key = :entityKey AND dskv.ds > :startDs AND dskv.ds < :endDs")
    CompletableFuture<DsKvEntity> findMax(@Param("entityId") String entityId,
                                          @Param("entityType") EntityType entityType,
                                          @Param("entityKey") String entityKey,
                                          @Param("startDs") Double startDs,
                                          @Param("endDs") Double endDs);

    @Async
    @Query("SELECT new DsKvEntity(MIN(dskv.strValue), MIN(dskv.longValue), MIN(dskv.doubleValue)) FROM DsKvEntity dskv " +
            "WHERE dskv.entityId = :entityId AND dskv.entityType = :entityType " +
            "AND dskv.key = :entityKey AND dskv.ds > :startDs AND dskv.ds < :endDs")
    CompletableFuture<DsKvEntity> findMin(@Param("entityId") String entityId,
                                          @Param("entityType") EntityType entityType,
                                          @Param("entityKey") String entityKey,
                                          @Param("startDs") Double startDs,
                                          @Param("endDs") Double endDs);

    @Async
    @Query("SELECT new DsKvEntity(COUNT(dskv.booleanValue), COUNT(dskv.strValue), COUNT(dskv.longValue), COUNT(dskv.doubleValue)) FROM DsKvEntity dskv " +
            "WHERE dskv.entityId = :entityId AND dskv.entityType = :entityType " +
            "AND dskv.key = :entityKey AND dskv.ds > :startDs AND dskv.ds < :endDs")
    CompletableFuture<DsKvEntity> findCount(@Param("entityId") String entityId,
                                            @Param("entityType") EntityType entityType,
                                            @Param("entityKey") String entityKey,
                                            @Param("startDs") Double startDs,
                                            @Param("endDs") Double endDs);

    @Async
    @Query("SELECT new DsKvEntity(AVG(dskv.longValue), AVG(dskv.doubleValue)) FROM DsKvEntity dskv " +
            "WHERE dskv.entityId = :entityId AND dskv.entityType = :entityType " +
            "AND dskv.key = :entityKey AND dskv.ds > :startDs AND dskv.ds < :endDs")
    CompletableFuture<DsKvEntity> findAvg(@Param("entityId") String entityId,
                                          @Param("entityType") EntityType entityType,
                                          @Param("entityKey") String entityKey,
                                          @Param("startDs") Double startDs,
                                          @Param("endDs") Double endDs);


    @Async
    @Query("SELECT new DsKvEntity(SUM(dskv.longValue), SUM(dskv.doubleValue)) FROM DsKvEntity dskv " +
            "WHERE dskv.entityId = :entityId AND dskv.entityType = :entityType " +
            "AND dskv.key = :entityKey AND dskv.ds > :startDs AND dskv.ds < :endDs")
    CompletableFuture<DsKvEntity> findSum(@Param("entityId") String entityId,
                                          @Param("entityType") EntityType entityType,
                                          @Param("entityKey") String entityKey,
                                          @Param("startDs") Double startDs,
                                          @Param("endDs") Double endDs);
}
