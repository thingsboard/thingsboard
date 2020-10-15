/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.transport.limits;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.msg.tools.TbRateLimits;

@Slf4j
@Component
public class DefaultTransportRateLimitFactory implements TransportRateLimitFactory {

    private static final DummyTransportRateLimit ALWAYS_TRUE = new DummyTransportRateLimit();

    @Override
    public TransportRateLimit create(TransportRateLimitType type, Object configuration) {
        if (!StringUtils.isEmpty(configuration)) {
            try {
                return new SimpleTransportRateLimit(new TbRateLimits(configuration.toString()), configuration.toString());
            } catch (Exception e) {
                log.warn("[{}] Failed to init rate limit with configuration: {}", type, configuration, e);
                return ALWAYS_TRUE;
            }
        } else {
            return ALWAYS_TRUE;
        }
    }

    @Override
    public TransportRateLimit createDefault(TransportRateLimitType type) {
        return ALWAYS_TRUE;
    }
}
