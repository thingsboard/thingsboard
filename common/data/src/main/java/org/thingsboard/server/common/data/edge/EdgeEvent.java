// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edge;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.thingsboard.server.common.data.BaseData;
import org.thingsboard.server.common.data.id.EdgeEventId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;
import java.util.UUID;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class EdgeEvent extends BaseData<EdgeEventId> {

    @Serial
    private static final long serialVersionUID = 5548866356798094088L;

    private long seqId;
    private TenantId tenantId;
    private EdgeId edgeId;
    private EdgeEventActionType action;
    private UUID entityId;
    private String uid;
    private EdgeEventType type;
    private transient JsonNode body;

    public EdgeEvent() {
        super();
    }

    public EdgeEvent(EdgeEventId id) {
        super(id);
    }

}
