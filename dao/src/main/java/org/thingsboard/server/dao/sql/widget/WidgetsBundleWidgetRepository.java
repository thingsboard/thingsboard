/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.widget;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetCompositeKey;
import org.thingsboard.server.dao.model.sql.WidgetsBundleWidgetEntity;

import java.util.List;
import java.util.UUID;

public interface WidgetsBundleWidgetRepository extends JpaRepository<WidgetsBundleWidgetEntity, WidgetsBundleWidgetCompositeKey> {

    List<WidgetsBundleWidgetEntity> findAllByWidgetsBundleId(UUID widgetsBundleId);

    @Query("SELECT w FROM WidgetsBundleWidgetEntity w WHERE w.widgetsBundleId IN (SELECT b.id FROM WidgetsBundleEntity b WHERE b.tenantId = :tenantId)")
    Page<WidgetsBundleWidgetEntity> findByTenantId(@Param("tenantId") UUID tenantId, Pageable pageable);

}
