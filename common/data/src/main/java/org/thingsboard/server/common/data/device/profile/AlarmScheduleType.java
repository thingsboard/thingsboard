// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(hidden = true)
@Deprecated
public enum AlarmScheduleType {

    ANY_TIME,
    SPECIFIC_TIME,
    CUSTOM

}
