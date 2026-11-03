// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.edge;

import lombok.Data;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;

import java.io.Serial;

@Data
public class EdgeHighPriorityMsg implements EdgeSessionMsg {

    @Serial
    private static final long serialVersionUID = 2703437686242033551L;

    private final TenantId tenantId;
    private final EdgeEvent edgeEvent;

    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_HIGH_PRIORITY_TO_EDGE_SESSION_MSG;
    }

}
