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
package org.thingsboard.server.transport.coap.callback;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.AbstractCoapTransportResource;

import java.util.UUID;
import java.util.function.BiConsumer;

@Slf4j
public class CoapDeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
    private final TransportContext transportContext;
    private final CoapExchange exchange;
    private final BiConsumer<TransportProtos.SessionInfoProto, DeviceProfile> onSuccess;

    public CoapDeviceAuthCallback(TransportContext transportContext, CoapExchange exchange, BiConsumer<TransportProtos.SessionInfoProto, DeviceProfile> onSuccess) {
        this.transportContext = transportContext;
        this.exchange = exchange;
        this.onSuccess = onSuccess;
    }

    @Override
    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
        DeviceProfile deviceProfile = msg.getDeviceProfile();
        if (msg.hasDeviceInfo() && deviceProfile != null) {
            TransportProtos.SessionInfoProto sessionInfoProto = SessionInfoCreator.create(msg, transportContext, UUID.randomUUID());
            onSuccess.accept(sessionInfoProto, deviceProfile);
        } else {
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
        }
    }

    @Override
    public void onError(Throwable e) {
        log.warn("Failed to process request", e);
        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
    }
}
