// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.limits;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.msg.tools.TbRateLimits;

@RequiredArgsConstructor
public class SimpleTransportRateLimit implements TransportRateLimit {

    private final TbRateLimits rateLimit;
    @Getter
    private final String configuration;

    public SimpleTransportRateLimit(String configuration) {
        this.configuration = configuration;
        this.rateLimit = new TbRateLimits(configuration);
    }

    @Override
    public boolean tryConsume() {
        return rateLimit.tryConsume();
    }

    @Override
    public boolean tryConsume(long number) {
        return number <= 0 || rateLimit.tryConsume(number);
    }
}
