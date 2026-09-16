// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.cf;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;

@Data
public class CalculatedFieldEntityLifecycleMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;
    private final ComponentLifecycleMsg data;

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_ENTITY_LIFECYCLE_MSG;
    }
}
