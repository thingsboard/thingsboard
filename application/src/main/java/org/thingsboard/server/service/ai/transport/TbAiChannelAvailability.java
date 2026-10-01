// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.ai.transport;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thingsboard.server.queue.util.TbCoreComponent;

import java.time.Clock;
import java.time.Duration;

@Component
@TbCoreComponent
public class TbAiChannelAvailability {

    private final boolean enabled;
    private final Duration fallbackBackoff;
    private final Clock clock;

    private volatile long unavailableUntil;

    @Autowired
    TbAiChannelAvailability(@Value("${TB_AI_CHANNEL_ENABLED:true}") boolean enabled,
                            @Value("${TB_AI_CHANNEL_FALLBACK_BACKOFF_MINUTES:10}") long fallbackBackoffMinutes) {
        this(enabled, Duration.ofMinutes(fallbackBackoffMinutes), Clock.systemUTC());
    }

    TbAiChannelAvailability(boolean enabled, Duration fallbackBackoff, Clock clock) {
        this.enabled = enabled;
        this.fallbackBackoff = fallbackBackoff;
        this.clock = clock;
    }

    public boolean isUsable() {
        return enabled && clock.millis() >= unavailableUntil;
    }

    public void markUnavailable() {
        unavailableUntil = clock.millis() + fallbackBackoff.toMillis();
    }

    public Duration fallbackBackoff() {
        return fallbackBackoff;
    }

}
