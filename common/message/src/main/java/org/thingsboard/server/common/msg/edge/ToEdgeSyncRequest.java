/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.msg.edge;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;
import org.thingsboard.server.common.msg.cluster.ToAllNodesMsg;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class ToEdgeSyncRequest implements TenantAwareMsg, ToAllNodesMsg {
    private final UUID id;
    private final TenantId tenantId;
    private final EdgeId edgeId;

    @Override
    public MsgType getMsgType() {
        return MsgType.EDGE_SYNC_REQUEST_TO_EDGE_SESSION_MSG;
    }
}
