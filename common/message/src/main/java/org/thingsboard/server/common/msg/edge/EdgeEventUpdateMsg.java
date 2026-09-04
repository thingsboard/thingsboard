// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.edge;

import lombok.Data;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;

import java.io.Serial;

@Data
public class EdgeEventUpdateMsg implements EdgeSessionMsg {

    @Serial
    private static final long serialVersionUID = -8050114506822836537L;

    private final TenantId tenantId;
    private final EdgeId edgeId;

    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_EVENT_UPDATE_TO_EDGE_SESSION_MSG;
    }

}
