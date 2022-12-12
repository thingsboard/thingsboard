/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.entity;

import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;

import java.util.Optional;

import static org.thingsboard.server.common.data.id.EntityId.NULL_UUID;

public interface TbEntityService {

    CustomerId NULL_CUSTOMER_ID = new CustomerId(NULL_UUID);

    Optional<HasId<?>> fetchEntity(TenantId tenantId, EntityId entityId);

    default CustomerId getCustomerId(TenantId tenantId, EntityId entityId) {
        return NULL_CUSTOMER_ID;
    }

}
