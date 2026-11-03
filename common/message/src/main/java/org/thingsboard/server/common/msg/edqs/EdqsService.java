// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.edqs;

import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.EdqsState;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsMsg;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

public interface EdqsService {

    void onUpdate(TenantId tenantId, EntityId entityId, Object entity);

    void onUpdate(TenantId tenantId, ObjectType objectType, EdqsObject object);

    void onDelete(TenantId tenantId, EntityId entityId);

    void onDelete(TenantId tenantId, ObjectType objectType, EdqsObject object);

    void processSystemRequest(ToCoreEdqsRequest request);

    void processSystemMsg(ToCoreEdqsMsg request);

    boolean isApiEnabled();

    EdqsState getState();

}
