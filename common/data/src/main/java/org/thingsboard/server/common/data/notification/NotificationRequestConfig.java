// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.notification;

import jakarta.validation.constraints.Max;
import lombok.Data;
import org.thingsboard.server.common.data.id.ReportId;

import java.util.List;

@Data
public class NotificationRequestConfig {

    @Max(value = MAX_SENDING_DELAY, message = "cannot be longer than 1 week")
    private int sendingDelayInSec;

    private List<ReportId> reports;

    public static final int MAX_SENDING_DELAY = 604800;

}
