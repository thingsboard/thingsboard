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
package org.thingsboard.server.extensions.api.plugins.ws.msg;

import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.plugins.PluginApiCallSecurityContext;
import org.thingsboard.server.extensions.api.plugins.ws.PluginWebsocketSessionRef;

public abstract class AbstractPluginWebSocketMsg<T> implements PluginWebsocketMsg<T> {

    private static final long serialVersionUID = 1L;

    private final PluginWebsocketSessionRef sessionRef;
    private final T payload;

    AbstractPluginWebSocketMsg(PluginWebsocketSessionRef sessionRef, T payload) {
        this.sessionRef = sessionRef;
        this.payload = payload;
    }

    public PluginWebsocketSessionRef getSessionRef() {
        return sessionRef;
    }


    @Override
    public TenantId getPluginTenantId(){
        return sessionRef.getPluginTenantId();
    }

    @Override
    public PluginId getPluginId() {
        return sessionRef.getPluginId();
    }

    @Override
    public PluginApiCallSecurityContext getSecurityCtx() {
        return sessionRef.getSecurityCtx();
    }

    public T getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "AbstractPluginWebSocketMsg [sessionRef=" + sessionRef + ", payload=" + payload + "]";
    }

}
