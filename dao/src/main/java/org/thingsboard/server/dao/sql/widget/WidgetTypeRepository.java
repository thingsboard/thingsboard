/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import org.springframework.data.repository.CrudRepository;
import org.thingsboard.server.dao.model.sql.WidgetTypeEntity;

import java.util.List;
import java.util.UUID;

/**
 * Created by Valerii Sosliuk on 4/29/2017.
 */
public interface WidgetTypeRepository extends CrudRepository<WidgetTypeEntity, UUID> {

    List<WidgetTypeEntity> findByTenantIdAndBundleAlias(UUID tenantId, String bundleAlias);

    WidgetTypeEntity findByTenantIdAndBundleAliasAndAlias(UUID tenantId, String bundleAlias, String alias);
}
