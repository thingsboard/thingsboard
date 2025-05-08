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
package org.thingsboard.server.common.transport.activity.strategy;

import lombok.EqualsAndHashCode;
import lombok.ToString;

@ToString
@EqualsAndHashCode
public final class FirstAndLastEventActivityStrategy implements ActivityStrategy {

    private boolean firstEventReceived;

    @Override
    public synchronized boolean onActivity() {
        if (!firstEventReceived) {
            firstEventReceived = true;
            return true;
        }
        return false;
    }

    @Override
    public synchronized boolean onReportingPeriodEnd() {
        firstEventReceived = false;
        return true;
    }

}
