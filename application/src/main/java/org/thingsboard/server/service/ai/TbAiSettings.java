// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

@Getter
@Component
@TbCoreComponent
public class TbAiSettings {

    private final boolean enabled;
    private final int sseInactivityTimeoutSeconds;
    private final int sseLogMaxDataLength;

    public TbAiSettings(@Value("${ai.enabled:true}") boolean enabled,
                        @Value("${ai.sse.inactivity_timeout:600}") int sseInactivityTimeoutSeconds,
                        @Value("${ai.sse.log_max_data_length:150}") int sseLogMaxDataLength) {
        this.enabled = enabled;
        this.sseInactivityTimeoutSeconds = sseInactivityTimeoutSeconds;
        this.sseLogMaxDataLength = sseLogMaxDataLength;
    }

}
