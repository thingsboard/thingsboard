// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification;

import jakarta.validation.constraints.Max;
import lombok.Data;

@Data
public class NotificationRequestConfig {

    @Max(value = MAX_SENDING_DELAY, message = "cannot be longer than 1 week")
    private int sendingDelayInSec;

    public static final int MAX_SENDING_DELAY = 604800;

}
