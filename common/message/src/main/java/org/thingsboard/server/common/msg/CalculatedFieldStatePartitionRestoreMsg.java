// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg;

import lombok.Data;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;

@Data
public class CalculatedFieldStatePartitionRestoreMsg implements ToCalculatedFieldSystemMsg {

    private final TopicPartitionInfo partition;

    @Override
    public TenantId getTenantId() {
        return TenantId.SYS_TENANT_ID;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.CF_STATE_PARTITION_RESTORE_MSG;
    }

}
