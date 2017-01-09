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
package org.thingsboard.server.extensions.api.plugins.ws;

public class WsSessionMetaData {

    private PluginWebsocketSessionRef sessionRef;
    private long lastActivityTime;

    public WsSessionMetaData(PluginWebsocketSessionRef sessionRef) {
        super();
        this.sessionRef = sessionRef;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public PluginWebsocketSessionRef getSessionRef() {
        return sessionRef;
    }

    public void setSessionRef(PluginWebsocketSessionRef sessionRef) {
        this.sessionRef = sessionRef;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

    public void setLastActivityTime(long lastActivityTime) {
        this.lastActivityTime = lastActivityTime;
    }

    @Override
    public String toString() {
        return "WsSessionMetaData [sessionRef=" + sessionRef + ", lastActivityTime=" + lastActivityTime + "]";
    }

}
