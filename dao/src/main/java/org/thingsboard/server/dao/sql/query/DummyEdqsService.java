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
package org.thingsboard.server.dao.sql.query;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.EdqsState;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsMsg;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.edqs.EdqsService;

@Service
@ConditionalOnMissingBean(value = EdqsService.class, ignored = DummyEdqsService.class)
public class DummyEdqsService implements EdqsService {

    @Override
    public void onUpdate(TenantId tenantId, EntityId entityId, Object entity) {}

    @Override
    public void onUpdate(TenantId tenantId, ObjectType objectType, EdqsObject object) {}

    @Override
    public void onDelete(TenantId tenantId, EntityId entityId) {}

    @Override
    public void onDelete(TenantId tenantId, ObjectType objectType, EdqsObject object) {}

    @Override
    public void processSystemRequest(ToCoreEdqsRequest request) {}

    @Override
    public void processSystemMsg(ToCoreEdqsMsg request) {}

    @Override
    public boolean isApiEnabled() {
        return getState().isApiEnabled();
    }

    @Override
    public EdqsState getState() {
        return new EdqsState();
    }

}
