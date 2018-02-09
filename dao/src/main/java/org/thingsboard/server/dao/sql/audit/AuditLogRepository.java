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
package org.thingsboard.server.dao.sql.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.dao.model.sql.AuditLogEntity;

import java.util.List;

public interface AuditLogRepository extends CrudRepository<AuditLogEntity, String> {

    @Query("SELECT al FROM AuditLogEntity al WHERE al.tenantId = :tenantId " +
            "AND al.id > :idOffset ORDER BY al.id")
    List<AuditLogEntity> findByTenantId(@Param("tenantId") String tenantId,
                                        @Param("idOffset") String idOffset,
                                        Pageable pageable);

    @Query("SELECT al FROM AuditLogEntity al WHERE al.tenantId = :tenantId " +
            "AND al.entityType = :entityType " +
            "AND al.entityId = :entityId " +
            "AND al.id > :idOffset ORDER BY al.id")
    List<AuditLogEntity> findByTenantIdAndEntityId(@Param("tenantId") String tenantId,
                                                   @Param("entityId") String entityId,
                                                   @Param("entityType") EntityType entityType,
                                                   @Param("idOffset") String idOffset,
                                                   Pageable pageable);
}
