// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.callback;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.transport.TransportServiceCallback;

public class CoapNoOpCallback implements TransportServiceCallback<Void> {
    private final CoapExchange exchange;

    public CoapNoOpCallback(CoapExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void onSuccess(Void msg) {
    }

    @Override
    public void onError(Throwable e) {
        exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
    }
}
