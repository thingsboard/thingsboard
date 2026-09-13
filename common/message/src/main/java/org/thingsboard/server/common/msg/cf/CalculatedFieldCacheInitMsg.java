// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.cf;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;

@Data
public class CalculatedFieldCacheInitMsg implements ToCalculatedFieldSystemMsg {

    private final TenantId tenantId;

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_CACHE_INIT_MSG;
    }

}
