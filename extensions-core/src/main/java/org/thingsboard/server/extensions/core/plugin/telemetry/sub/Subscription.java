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
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;

import java.util.Map;

@Data
@AllArgsConstructor
public class Subscription {

    private final SubscriptionState sub;
    private final boolean local;
    private ServerAddress server;

    public Subscription(SubscriptionState sub, boolean local) {
        this(sub, local, null);
    }

    public String getWsSessionId() {
        return getSub().getWsSessionId();
    }

    public int getSubscriptionId() {
        return getSub().getSubscriptionId();
    }

    public EntityId getEntityId() {
        return getSub().getEntityId();
    }

    public SubscriptionType getType() {
        return getSub().getType();
    }

    public boolean isAllKeys() {
        return getSub().isAllKeys();
    }

    public Map<String, Long> getKeyStates() {
        return getSub().getKeyStates();
    }

    public void setKeyState(String key, long ts) {
        getSub().getKeyStates().put(key, ts);
    }

    @Override
    public String toString() {
        return "Subscription{" +
                "sub=" + sub +
                ", local=" + local +
                ", server=" + server +
                '}';
    }
}
