// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.query;

import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;

@Data

public class SchedulerEventFilter implements EntityFilter {

    private AliasEntityId originator;
    private String eventType;
    private boolean originatorStateEntity;
    private AliasEntityId defaultStateEntity;

    @Override
    public EntityFilterType getType() {
        return EntityFilterType.SCHEDULER_EVENT;
    }
}
