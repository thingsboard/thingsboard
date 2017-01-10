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

import org.thingsboard.server.common.data.id.PluginId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.extensions.api.plugins.PluginApiCallSecurityContext;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Map;

public class BasicPluginWebsocketSessionRef implements PluginWebsocketSessionRef {

    private static final long serialVersionUID = 1L;

    private final String sessionId;
    private final PluginApiCallSecurityContext securityCtx;
    private final URI uri;
    private final Map<String, Object> attributes;
    private final InetSocketAddress localAddress;
    private final InetSocketAddress remoteAddress;

    public BasicPluginWebsocketSessionRef(String sessionId, PluginApiCallSecurityContext securityCtx, URI uri, Map<String, Object> attributes,
            InetSocketAddress localAddress, InetSocketAddress remoteAddress) {
        super();
        this.sessionId = sessionId;
        this.securityCtx = securityCtx;
        this.uri = uri;
        this.attributes = attributes;
        this.localAddress = localAddress;
        this.remoteAddress = remoteAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public TenantId getPluginTenantId() {
        return securityCtx.getPluginTenantId();
    }

    public PluginId getPluginId() {
        return securityCtx.getPluginId();
    }

    public URI getUri() {
        return uri;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public InetSocketAddress getLocalAddress() {
        return localAddress;
    }

    public InetSocketAddress getRemoteAddress() {
        return remoteAddress;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sessionId == null) ? 0 : sessionId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BasicPluginWebsocketSessionRef other = (BasicPluginWebsocketSessionRef) obj;
        if (sessionId == null) {
            if (other.sessionId != null)
                return false;
        } else if (!sessionId.equals(other.sessionId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "BasicPluginWebsocketSessionRef [sessionId=" + sessionId + ", pluginId=" + getPluginId() + "]";
    }

    @Override
    public PluginApiCallSecurityContext getSecurityCtx() {
        return securityCtx;
    }

}
