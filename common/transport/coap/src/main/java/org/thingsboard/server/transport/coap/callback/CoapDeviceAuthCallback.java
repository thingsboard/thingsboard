// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.transport.coap.callback;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.transport.coap.CoapTransportContext;

import java.util.function.BiConsumer;

@Slf4j
public class CoapDeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
    private final CoapTransportContext transportContext;
    private final CoapExchange exchange;
    private final BiConsumer<ValidateDeviceCredentialsResponse, DeviceProfile> onSuccess;

    public CoapDeviceAuthCallback(CoapTransportContext transportContext, CoapExchange exchange, BiConsumer<ValidateDeviceCredentialsResponse, DeviceProfile> onSuccess) {
        this.transportContext = transportContext;
        this.exchange = exchange;
        this.onSuccess = onSuccess;
    }

    @Override
    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
        DeviceProfile deviceProfile = msg.getDeviceProfile();
        if (msg.hasDeviceInfo() && deviceProfile != null) {
            transportContext.onAuthSuccess(exchange.advanced().getRequest());
            onSuccess.accept(msg, deviceProfile);
        } else {
            if (!msg.hasDeviceInfo()) {
                transportContext.onAuthFailure(exchange.advanced().getRequest());
            }
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
        }
    }

    @Override
    public void onError(Throwable e) {
        log.warn("Failed to process request", e);
        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
    }
}
