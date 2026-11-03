// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.edge;

import lombok.Data;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;

import java.io.Serial;
import java.util.UUID;

@Data
public class ToEdgeSyncRequest implements EdgeSessionMsg {

    @Serial
    private static final long serialVersionUID = -7624597032448212259L;

    private final UUID id;
    private final TenantId tenantId;
    private final EdgeId edgeId;
    private final String serviceId;

    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG;
    }

}
