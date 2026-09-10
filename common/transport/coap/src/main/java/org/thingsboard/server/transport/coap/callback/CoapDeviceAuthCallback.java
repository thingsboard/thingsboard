// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.callback;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;

import java.util.function.BiConsumer;

@Slf4j
public class CoapDeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
    private final CoapExchange exchange;
    private final BiConsumer<ValidateDeviceCredentialsResponse, DeviceProfile> onSuccess;

    public CoapDeviceAuthCallback(CoapExchange exchange, BiConsumer<ValidateDeviceCredentialsResponse, DeviceProfile> onSuccess) {
        this.exchange = exchange;
        this.onSuccess = onSuccess;
    }

    @Override
    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
        DeviceProfile deviceProfile = msg.getDeviceProfile();
        if (msg.hasDeviceInfo() && deviceProfile != null) {
            onSuccess.accept(msg, deviceProfile);
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
