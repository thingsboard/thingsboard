// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import org.thingsboard.server.common.data.query.DynamicValue;

@Schema(hidden = true)
@Deprecated
public class AnyTimeSchedule implements AlarmSchedule {

    @Override
    public AlarmScheduleType getType() {
        return AlarmScheduleType.ANY_TIME;
    }

    @Override
    public DynamicValue<String> getDynamicValue() {
        return null;
    }

}
