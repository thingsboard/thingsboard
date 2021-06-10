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
package org.thingsboard.server.transport.lwm2m.server;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceObserver;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.FIRMWARE_UPDATE_COAP_RECOURSE;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.SOFTWARE_UPDATE_COAP_RECOURSE;

@Slf4j
public class LwM2mTransportCoapResource extends AbstractLwM2mTransportResource {
    private final ConcurrentMap<String, ObserveRelation> tokenToObserveRelationMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicInteger> tokenToObserveNotificationSeqMap = new ConcurrentHashMap<>();
    private final LwM2mTransportMsgHandler handler;

    public LwM2mTransportCoapResource(LwM2mTransportMsgHandler handler, String name) {
        super(name);
        this.handler = handler;
        this.setObservable(true); // enable observing
        this.addObserver(new CoapResourceObserver());
    }


    @Override
    public void checkObserveRelation(Exchange exchange, Response response) {
        String token = getTokenFromRequest(exchange.getRequest());
        final ObserveRelation relation = exchange.getRelation();
        if (relation == null || relation.isCanceled()) {
            return; // because request did not try to establish a relation
        }
        if (CoAP.ResponseCode.isSuccess(response.getCode())) {

            if (!relation.isEstablished()) {
                relation.setEstablished();
                addObserveRelation(relation);
            }
            AtomicInteger notificationCounter = tokenToObserveNotificationSeqMap.computeIfAbsent(token, s -> new AtomicInteger(0));
            response.getOptions().setObserve(notificationCounter.getAndIncrement());
        } // ObserveLayer takes care of the else case
    }


    @Override
    protected void processHandleGet(CoapExchange exchange) {
        log.warn("90) processHandleGet [{}]", exchange);
        if (exchange.getRequestOptions().getUriPath().size() >= 2 &&
                (FIRMWARE_UPDATE_COAP_RECOURSE.equals(exchange.getRequestOptions().getUriPath().get(exchange.getRequestOptions().getUriPath().size()-2)) ||
                        SOFTWARE_UPDATE_COAP_RECOURSE.equals(exchange.getRequestOptions().getUriPath().get(exchange.getRequestOptions().getUriPath().size()-2)))) {
            this.sendOtaData(exchange);
        }
    }

    @Override
    protected void processHandlePost(CoapExchange exchange) {
        log.warn("2) processHandleGet [{}]", exchange);
    }

    /**
     * Override the default behavior so that requests to sub resources (typically /{name}/{token}) are handled by
     * /name resource.
     */
    @Override
    public Resource getChild(String name) {
        return this;
    }


    private String getTokenFromRequest(Request request) {
        return (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getAddress().getHostAddress() : "null")
                + ":" + (request.getSourceContext() != null ? request.getSourceContext().getPeerAddress().getPort() : -1) + ":" + request.getTokenString();
    }

    public class CoapResourceObserver implements ResourceObserver {

        @Override
        public void changedName(String old) {

        }

        @Override
        public void changedPath(String old) {

        }

        @Override
        public void addedChild(Resource child) {

        }

        @Override
        public void removedChild(Resource child) {

        }

        @Override
        public void addedObserveRelation(ObserveRelation relation) {

        }

        @Override
        public void removedObserveRelation(ObserveRelation relation) {

        }
    }

    private void sendOtaData(CoapExchange exchange) {
        String idStr = exchange.getRequestOptions().getUriPath().get(exchange.getRequestOptions().getUriPath().size()-1
        );
        UUID currentId = UUID.fromString(idStr);
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        byte[] fwData = this.getOtaData(currentId);
        log.warn("91) read softWare data (length): [{}]", fwData.length);
        if (fwData != null && fwData.length > 0) {
            response.setPayload(fwData);
            if (exchange.getRequestOptions().getBlock2() != null) {
                int chunkSize = exchange.getRequestOptions().getBlock2().getSzx();
                boolean moreFlag = fwData.length > chunkSize;
                response.getOptions().setBlock2(chunkSize, moreFlag, 0);
                log.warn("92) with blokc2 Send currentId: [{}], length: [{}], chunkSize [{}], moreFlag [{}]", currentId.toString(), fwData.length, chunkSize, moreFlag);
            }
            else {
                log.warn("92) with block1 Send currentId: [{}], length: [{}], ", currentId.toString(), fwData.length);
            }
            exchange.respond(response);
        }
    }

    private byte[] getOtaData(UUID currentId) {
        return ((DefaultLwM2MTransportMsgHandler) handler).otaPackageDataCache.get(currentId.toString());
    }

}
