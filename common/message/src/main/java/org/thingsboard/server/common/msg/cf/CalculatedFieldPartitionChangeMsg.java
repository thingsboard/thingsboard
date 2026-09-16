// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.cf;

import lombok.Data;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.ToCalculatedFieldSystemMsg;

import java.util.Set;

@Data
public class CalculatedFieldPartitionChangeMsg implements ToCalculatedFieldSystemMsg {

    @Override
    public TenantId getTenantId() {
        return TenantId.SYS_TENANT_ID;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_PARTITIONS_CHANGE_MSG;
    }
}
