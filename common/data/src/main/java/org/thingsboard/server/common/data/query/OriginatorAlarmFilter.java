// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class OriginatorAlarmFilter {
    private EntityId originatorId;
    private List<String> typeList;
    private List<AlarmSeverity> severityList;
}
