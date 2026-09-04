// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.callback;

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.transport.TransportServiceCallback;

public class CoapResponseCodeCallback implements TransportServiceCallback<Void> {

    protected final CoapExchange exchange;
    protected final CoAP.ResponseCode onSuccessResponse;
    protected final CoAP.ResponseCode onFailureResponse;

    public CoapResponseCodeCallback(CoapExchange exchange, CoAP.ResponseCode onSuccessResponse, CoAP.ResponseCode onFailureResponse) {
        this.exchange = exchange;
        this.onSuccessResponse = onSuccessResponse;
        this.onFailureResponse = onFailureResponse;
    }

    @Override
    public void onSuccess(Void msg) {
        Response response = new Response(onSuccessResponse);
        response.setConfirmable(isConRequest());
        exchange.respond(response);
    }

    @Override
    public void onError(Throwable e) {
        exchange.respond(onFailureResponse);
    }

    protected boolean isConRequest() {
        return exchange.advanced().getRequest().isConfirmable();
    }
}
