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
package org.thingsboard.server.service.subscription;

import org.thingsboard.server.service.ws.WebSocketSessionRef;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.AlarmStatusCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityCountCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

public interface TbEntityDataSubscriptionService {

    void handleCmd(WebSocketSessionRef sessionId, EntityDataCmd cmd);

    void handleCmd(WebSocketSessionRef sessionId, EntityCountCmd cmd);

    void handleCmd(WebSocketSessionRef sessionId, AlarmDataCmd cmd);

    void handleCmd(WebSocketSessionRef sessionId, AlarmCountCmd cmd);

    void handleCmd(WebSocketSessionRef session, AlarmStatusCmd cmd);

    void cancelSubscription(String sessionId, UnsubscribeCmd subscriptionId);

    void cancelAllSessionSubscriptions(String sessionId);

}
