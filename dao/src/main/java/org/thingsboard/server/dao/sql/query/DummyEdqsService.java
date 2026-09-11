// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
