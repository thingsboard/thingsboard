/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.transport.TransportServiceCallback;

public class CoapEfentoCallback implements TransportServiceCallback<Void> {

    protected final CoapExchange exchange;
    protected final CoAP.ResponseCode onSuccessResponse;
    protected final CoAP.ResponseCode onFailureResponse;

    public CoapEfentoCallback(CoapExchange exchange, CoAP.ResponseCode onSuccessResponse, CoAP.ResponseCode onFailureResponse) {
        this.exchange = exchange;
        this.onSuccessResponse = onSuccessResponse;
        this.onFailureResponse = onFailureResponse;
    }

    @Override
    public void onSuccess(Void msg) {
        //We respond only to confirmed requests in order to reduce battery consumption for Efento devices.
        if (isConRequest()) {
            exchange.respond(new Response(onSuccessResponse));
        }
    }

    @Override
    public void onError(Throwable e) {
        exchange.respond(onFailureResponse);
    }

    protected boolean isConRequest() {
        return exchange.advanced().getRequest().isConfirmable();
    }
}
