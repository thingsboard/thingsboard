// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;

@Data
@Builder
public final class PropagationCalculatedFieldResult implements CalculatedFieldResult {

    private final List<EntityId> entityIds;
    private final TelemetryCalculatedFieldResult result;

    @Override
    public TbMsg toTbMsg(EntityId entityId, String cfName, List<CalculatedFieldId> cfIds) {
        return result.toTbMsg(entityId, cfName, cfIds);
    }

    @Override
    public String stringValue() {
        return result.stringValue();
    }

    @Override
    public boolean isEmpty() {
        return CollectionsUtil.isEmpty(entityIds) || result.isEmpty();
    }

}
