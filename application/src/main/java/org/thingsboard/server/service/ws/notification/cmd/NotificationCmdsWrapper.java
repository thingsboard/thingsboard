/**
 * Copyright © 2016-2023 The Thingsboard Authors
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.service.ws.telemetry.cmd.WsCmdsWrapper;

/**
 * @deprecated Use {@link org.thingsboard.server.service.ws.telemetry.cmd.WsCmdsWrapper}. This class is left for backward compatibility
 * */
@Data
@Deprecated
public class NotificationCmdsWrapper {

    private NotificationsCountSubCmd unreadCountSubCmd;

    private NotificationsSubCmd unreadSubCmd;

    private MarkNotificationsAsReadCmd markAsReadCmd;

    private MarkAllNotificationsAsReadCmd markAllAsReadCmd;

    private NotificationsUnsubCmd unsubCmd;

    @JsonIgnore
    public WsCmdsWrapper toCommonCmdsWrapper() {
        WsCmdsWrapper wrapper = new WsCmdsWrapper();
        wrapper.setUnreadNotificationsCountSubCmd(unreadCountSubCmd);
        wrapper.setUnreadNotificationsSubCmd(unreadSubCmd);
        wrapper.setMarkNotificationAsReadCmd(markAsReadCmd);
        wrapper.setMarkAllNotificationsAsReadCmd(markAllAsReadCmd);
        wrapper.setNotificationsUnsubCmd(unsubCmd);
        return wrapper;
    }

}
