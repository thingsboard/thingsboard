/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.service.ws.notification.cmd;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.thingsboard.server.service.ws.WsCmd;
import org.thingsboard.server.service.ws.WsCmdType;
import org.thingsboard.server.service.ws.telemetry.cmd.v2.UnsubscribeCmd;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationsUnsubCmd implements UnsubscribeCmd, WsCmd {
    private int cmdId;

    @Override
    public WsCmdType getType() {
        return WsCmdType.NOTIFICATIONS_UNSUBSCRIBE;
    }
}
