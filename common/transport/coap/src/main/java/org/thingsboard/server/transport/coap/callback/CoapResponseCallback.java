// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.callback;

import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.transport.TransportServiceCallback;

public class CoapResponseCallback implements TransportServiceCallback<Void> {

    protected final CoapExchange exchange;
    protected final Response onSuccessResponse;
    protected final Response onFailureResponse;

    public CoapResponseCallback(CoapExchange exchange, Response onSuccessResponse, Response onFailureResponse) {
        this.exchange = exchange;
        this.onSuccessResponse = onSuccessResponse;
        this.onFailureResponse = onFailureResponse;
    }

    /**
     * @param msg
     */
    @Override
    public void onSuccess(Void msg) {
        this.onSuccessResponse.setConfirmable(isConRequest());
        exchange.respond(this.onSuccessResponse);
    }

    /**
     * @param e
     */
    @Override
    public void onError(Throwable e) {
        exchange.respond(onFailureResponse);
    }

    protected boolean isConRequest() {
        return exchange.advanced().getRequest().isConfirmable();
    }
}
