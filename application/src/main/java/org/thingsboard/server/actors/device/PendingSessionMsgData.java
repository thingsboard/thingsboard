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
package org.thingsboard.server.actors.device;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.thingsboard.server.common.data.id.SessionId;
import org.thingsboard.server.common.msg.cluster.ServerAddress;
import org.thingsboard.server.common.msg.session.SessionMsgType;

import java.util.Optional;

/**
 * Created by ashvayka on 17.04.18.
 */
@Data
@AllArgsConstructor
public final class PendingSessionMsgData {

    private final SessionId sessionId;
    private final Optional<ServerAddress> serverAddress;
    private final SessionMsgType sessionMsgType;
    private final int requestId;
    private final boolean replyOnQueueAck;
    private int ackMsgCount;

}
