// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.callback;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.adaptor.AdaptorException;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.client.TbCoapClientState;

@Slf4j
public class ToServerRpcSyncSessionCallback extends AbstractSyncSessionCallback {

    public ToServerRpcSyncSessionCallback(TbCoapClientState state, CoapExchange exchange, Request request) {
        super(state, exchange, request);
    }

    @Override
    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        try {
            respond(state.getAdaptor().convertToPublish(toServerResponse));
        } catch (AdaptorException e) {
            log.trace("Failed to reply due to error", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }
}
