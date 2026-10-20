// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.util;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogSettingsComponent {

    @Value("${server.log_controller_error_stack_trace:true}")
    @Getter
    private boolean exceptionStackTraceEnabled;

}
