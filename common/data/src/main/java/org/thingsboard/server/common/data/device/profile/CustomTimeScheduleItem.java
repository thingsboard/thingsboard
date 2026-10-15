// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.device.profile;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Schema(hidden = true)
@Data
@Deprecated
public class CustomTimeScheduleItem implements Serializable {

    private boolean enabled;
    private int dayOfWeek;
    private long startsOn;
    private long endsOn;

}
