// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.alarm;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.TimePageLink;

@Data
@Builder
@AllArgsConstructor
public class AlarmQuery {

    private EntityId affectedEntityId;
    private TimePageLink pageLink;
    private AlarmSearchStatus searchStatus;
    private AlarmStatus status;
    private UserId assigneeId;
    @Deprecated
    private Boolean fetchOriginator;

}
