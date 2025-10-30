/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
import org.thingsboard.server.cache.ota.OtaPackageDataCache;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_UPDATE_COAP_RESOURCE;

@Slf4j
public class LwM2mTransportCoapResource extends AbstractLwM2mTransportResource {
    private final ConcurrentMap<String, AtomicInteger> tokenToObserveNotificationSeqMap = new ConcurrentHashMap<>();
    private final OtaPackageDataCache otaPackageDataCache;

    public LwM2mTransportCoapResource(OtaPackageDataCache otaPackageDataCache, String name) {
        super(name);
        this.otaPackageDataCache = otaPackageDataCache;
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
        if (response.getCode().isSuccess()) {

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
        log.debug("processHandleGet [{}]", exchange);
        List<String> uriPath = exchange.getRequestOptions().getUriPath();
        if (uriPath.size() >= 2 &&
                (FIRMWARE_UPDATE_COAP_RESOURCE.equals(uriPath.get(uriPath.size() - 2)) ||
                        SOFTWARE_UPDATE_COAP_RESOURCE.equals(uriPath.get(uriPath.size() - 2)))) {
            this.sendOtaData(exchange);
        }
    }

    @Override
    protected void processHandlePost(CoapExchange exchange) {
        log.debug("processHandlePost [{}]", exchange);
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
        String idStr = exchange.getRequestOptions().getUriPath().get(exchange.getRequestOptions().getUriPath().size() - 1
        );
        UUID currentId = UUID.fromString(idStr);
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        byte[] otaData = this.getOtaData(currentId);
        if (otaData != null && otaData.length > 0) {
            log.debug("Read ota data (length): [{}]", otaData.length);
            response.setPayload(otaData);
            if (exchange.getRequestOptions().getBlock2() != null) {
                int szx = exchange.getRequestOptions().getBlock2().getSzx();
                int chunkSize = exchange.getRequestOptions().getBlock2().getSize();
                boolean lastFlag = otaData.length <= chunkSize;
                response.getOptions().setBlock2(szx, lastFlag, 0);
                log.trace("With block2 Send currentId: [{}], length: [{}], chunkSize [{}], szx [{}], moreFlag [{}]", currentId, otaData.length, chunkSize, szx, lastFlag);
            } else {
                log.trace("With block1 Send currentId: [{}], length: [{}], ", currentId, otaData.length);
            }
            exchange.respond(response);
        } else {
            log.trace("Ota packaged currentId: [{}] is not found.", currentId);
        }
    }

    private byte[] getOtaData(UUID currentId) {
        return otaPackageDataCache.get(currentId.toString());
    }

}
