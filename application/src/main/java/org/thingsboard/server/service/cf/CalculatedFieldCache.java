// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.service.cf.ctx.state.CalculatedFieldCtx;

import java.util.List;

public interface CalculatedFieldCache {

    CalculatedField getCalculatedField(CalculatedFieldId calculatedFieldId);

    List<CalculatedField> getCalculatedFieldsByEntityId(EntityId entityId);

    List<CalculatedFieldLink> getCalculatedFieldLinksByEntityId(EntityId entityId);

    CalculatedFieldCtx getCalculatedFieldCtx(CalculatedFieldId calculatedFieldId);

    List<CalculatedFieldCtx> getCalculatedFieldCtxsByEntityId(EntityId entityId);

    void addCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void updateCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    void evict(CalculatedFieldId calculatedFieldId);

}
