/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.core.plugin.telemetry.sub;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.DeviceId;

import java.util.Map;

/**
 * @author Andrew Shvayka
 */
@Data
@AllArgsConstructor
public class SubscriptionState {

    private final String wsSessionId;
    private final int subscriptionId;
    private final DeviceId deviceId;
    private final SubscriptionType type;
    private final boolean allKeys;
    private final Map<String, Long> keyStates;

    @Override
    public String toString() {
        return "SubscriptionState{" +
                "type=" + type +
                ", deviceId=" + deviceId +
                ", subscriptionId=" + subscriptionId +
                ", wsSessionId='" + wsSessionId + '\'' +
                '}';
    }
}
