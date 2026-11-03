// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.housekeeper;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.Serial;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TsHistoryDeletionHousekeeperTask extends HousekeeperTask {

    @Serial
    private static final long serialVersionUID = 4573851542705079043L;

    private String key;

    public TsHistoryDeletionHousekeeperTask(TenantId tenantId, EntityId entityId, String key) {
        super(tenantId, entityId, HousekeeperTaskType.DELETE_TS_HISTORY);
        this.key = key;
    }

    @Override
    public String getDescription() {
        return super.getDescription() + (key != null ? " for key '" + key + "'" : "");
    }

}
