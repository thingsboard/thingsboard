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

import lombok.Data;
import lombok.ToString;

import java.util.Map;

@Data
@ToString
public class DepthSubscription extends Subscription{

    private final DepthSubscriptionState sub;

    public DepthSubscription(SubscriptionState sub, boolean local, DepthSubscriptionState depthSubscriptionState) {
        super(sub, local);
        this.sub = depthSubscriptionState;
    }

    public Map<String, Double> getDepthKeyStates() { return getSub().getDepthKeyStates(); }

    public void setDepthKeyState(String key, Double ds) {
        getSub().getDepthKeyStates().put(key, ds);
    }

}
