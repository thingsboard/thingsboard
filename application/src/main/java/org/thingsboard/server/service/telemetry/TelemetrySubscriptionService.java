/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.service.telemetry;

import org.thingsboard.rule.engine.api.RuleEngineTelemetryService;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.service.telemetry.sub.SubscriptionState;

/**
 * Created by ashvayka on 27.03.18.
 */
public interface TelemetrySubscriptionService extends RuleEngineTelemetryService {

    void addLocalWsSubscription(String sessionId, EntityId entityId, SubscriptionState sub);

    void cleanupLocalWsSessionSubscriptions(TelemetryWebSocketSessionRef sessionRef, String sessionId);

    void removeSubscription(String sessionId, int cmdId);

    void onNewRemoteSubscription(ServerAddress serverAddress, byte[] data);

    void onRemoteSubscriptionUpdate(ServerAddress serverAddress, byte[] bytes);

    void onRemoteSubscriptionClose(ServerAddress serverAddress, byte[] bytes);

    void onRemoteSessionClose(ServerAddress serverAddress, byte[] bytes);

    void onRemoteAttributesUpdate(ServerAddress serverAddress, byte[] bytes);

    void onRemoteTsUpdate(ServerAddress serverAddress, byte[] bytes);

    void onClusterUpdate();
}
