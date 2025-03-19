/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.mockito.BDDMockito;
import org.mockito.Mockito;

import java.util.concurrent.TimeUnit;

@Slf4j
class ReconnectStrategyExponentialTest {

    @Test
    public void exponentialReconnect() {
        ReconnectStrategyExponential strategy = Mockito.spy(new ReconnectStrategyExponential(1));
        for (int i = 0; i < 10; i++) {
            log.info("Disconnect [{}] Delay [{}]", i, strategy.getNextReconnectDelay());
        }

        final long coolDownPeriod = strategy.getReconnectIntervalMinSeconds() + strategy.getReconnectIntervalMaxSeconds() + 1;

        BDDMockito.willAnswer((x) -> System.nanoTime() + TimeUnit.SECONDS.toNanos(coolDownPeriod)).given(strategy).getNanoTime();
        log.info("After cooldown period [{}] seconds later...", coolDownPeriod);
        for (int i = 0; i < 10; i++) {
            log.info("Disconnect [{}] Delay [{}]", i, strategy.getNextReconnectDelay());
        }
    }
}