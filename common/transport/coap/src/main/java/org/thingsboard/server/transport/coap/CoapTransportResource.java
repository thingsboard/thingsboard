/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.msg.session.FeatureType;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.adaptors.CoapTransportAdaptor;

import java.util.List;
import java.util.Optional;

@Slf4j
public class CoapTransportResource extends CoapTransportRootResource {

    public CoapTransportResource(CoapTransportContext context, String name) {
        super(context, name);
    }

    @Override
    protected void processHandleGet(CoapExchange exchange) {
        doProcessHandleGet(exchange);
    }

    @Override
    protected void processHandlePost(CoapExchange exchange) {
        doProcessHandlePost(exchange);
    }

    @Override
    protected Optional<FeatureType> processGetFeatureType(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        try {
            return toFeatureType(uriPath);
        } catch (RuntimeException e) {
            log.warn("Failed to decode feature type: {}", uriPath);
        }
        return Optional.empty();
    }

    @Override
    public Resource getChild(String name) {
        return this;
    }

    @Override
    protected void registerAsyncCoapSession(CoapExchange exchange, TransportProtos.SessionInfoProto sessionInfo, CoapTransportAdaptor coapTransportAdaptor, String token) {
        tokenToSessionIdMap.putIfAbsent(token, sessionInfo);
        CoapSessionListener attrListener = new CoapSessionListener(this, exchange, coapTransportAdaptor);
        transportService.registerAsyncSession(sessionInfo, attrListener);
        transportService.process(sessionInfo, getSessionEventMsg(TransportProtos.SessionEvent.OPEN), null);
    }

    protected Optional<DeviceTokenCredentials> decodeCredentials(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        return toDeviceTokenCredentials(uriPath);
    }
}
