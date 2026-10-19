// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;

@Slf4j
public abstract class AbstractCoapTransportResource extends CoapResource {

    protected final CoapTransportContext transportContext;
    protected final TransportService transportService;

    public AbstractCoapTransportResource(CoapTransportContext context, String name) {
        super(name);
        this.transportContext = context;
        this.transportService = context.getTransportService();
    }

    @Override
    public void handleGET(CoapExchange exchange) {
        processHandleGet(exchange);
    }

    @Override
    public void handlePOST(CoapExchange exchange) {
        processHandlePost(exchange);
    }

    protected abstract void processHandleGet(CoapExchange exchange);

    protected abstract void processHandlePost(CoapExchange exchange);

    protected void reportSubscriptionInfo(TransportProtos.SessionInfoProto sessionInfo, boolean hasAttributeSubscription, boolean hasRpcSubscription) {
        transportContext.getTransportService().process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(hasAttributeSubscription)
                .setRpcSubscription(hasRpcSubscription)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
    }

}
