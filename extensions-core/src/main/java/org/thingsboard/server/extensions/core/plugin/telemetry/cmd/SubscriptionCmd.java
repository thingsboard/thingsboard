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
package org.thingsboard.server.extensions.core.plugin.telemetry.cmd;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.thingsboard.server.extensions.core.plugin.telemetry.sub.SubscriptionType;

@NoArgsConstructor
@AllArgsConstructor
public abstract class SubscriptionCmd implements TelemetryPluginCmd {

    private int cmdId;
    private String deviceId;
    private String keys;
    private String scope;
    private boolean unsubscribe;

    public abstract SubscriptionType getType();

    public int getCmdId() {
        return cmdId;
    }

    public void setCmdId(int cmdId) {
        this.cmdId = cmdId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getKeys() {
        return keys;
    }

    public void setTags(String tags) {
        this.keys = tags;
    }

    public boolean isUnsubscribe() {
        return unsubscribe;
    }

    public void setUnsubscribe(boolean unsubscribe) {
        this.unsubscribe = unsubscribe;
    }

    public String getScope() {
        return scope;
    }

    public void setKeys(String keys) {
        this.keys = keys;
    }

    @Override
    public String toString() {
        return "SubscriptionCmd [deviceId=" + deviceId + ", tags=" + keys + ", unsubscribe=" + unsubscribe + "]";
    }

}
