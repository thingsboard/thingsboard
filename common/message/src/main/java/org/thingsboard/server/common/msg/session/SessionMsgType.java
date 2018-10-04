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
package org.thingsboard.server.common.msg.session;

public enum SessionMsgType {
    GET_ATTRIBUTES_REQUEST(true), POST_ATTRIBUTES_REQUEST(true), GET_ATTRIBUTES_RESPONSE,
    SUBSCRIBE_ATTRIBUTES_REQUEST, UNSUBSCRIBE_ATTRIBUTES_REQUEST, ATTRIBUTES_UPDATE_NOTIFICATION,

    POST_TELEMETRY_REQUEST(true), STATUS_CODE_RESPONSE,

    SUBSCRIBE_RPC_COMMANDS_REQUEST, UNSUBSCRIBE_RPC_COMMANDS_REQUEST,
    TO_DEVICE_RPC_REQUEST, TO_DEVICE_RPC_RESPONSE, TO_DEVICE_RPC_RESPONSE_ACK,

    TO_SERVER_RPC_REQUEST(true), TO_SERVER_RPC_RESPONSE,

    RULE_ENGINE_ERROR,

    SESSION_OPEN, SESSION_CLOSE;

    private final boolean requiresRulesProcessing;

    SessionMsgType() {
        this(false);
    }

    SessionMsgType(boolean requiresRulesProcessing) {
        this.requiresRulesProcessing = requiresRulesProcessing;
    }

    public boolean requiresRulesProcessing() {
        return requiresRulesProcessing;
    }
}
