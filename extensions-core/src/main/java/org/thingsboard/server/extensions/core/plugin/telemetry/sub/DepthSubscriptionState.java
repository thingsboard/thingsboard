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

import lombok.Getter;
import org.thingsboard.server.common.data.id.EntityId;

import java.util.Map;

public class DepthSubscriptionState extends SubscriptionState{

    @Getter
    private final Map<String, Double> depthKeyStates;
    public DepthSubscriptionState(String wsSessionId, int subscriptionId, EntityId entityId, SubscriptionType type,
                                  boolean allKeys, Map<String, Long> keyStates, Map<String, Double> depthKeyStates) {
        super(wsSessionId, subscriptionId, entityId, type, allKeys, keyStates);
        this.depthKeyStates = depthKeyStates;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DepthSubscriptionState that = (DepthSubscriptionState) o;

        if (subscriptionId != that.subscriptionId) return false;
        if (wsSessionId != null ? !wsSessionId.equals(that.wsSessionId) : that.wsSessionId != null) return false;
        if (entityId != null ? !entityId.equals(that.entityId) : that.entityId != null) return false;
        return type == that.type;

    }

    @Override
    public int hashCode() {
        int result = wsSessionId != null ? wsSessionId.hashCode() : 0;
        result = 31 * result + subscriptionId;
        result = 31 * result + (entityId != null ? entityId.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "DepthSubscriptionState{" +
                "type=" + type +
                ", entityId=" + entityId +
                ", subscriptionId=" + subscriptionId +
                ", wsSessionId='" + wsSessionId + '\'' +
                '}';
    }
}
