/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.transport.lwm2m.server.coapresource;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.observe.ObserveRelation;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.eclipse.californium.core.server.resources.ResourceObserver;
import org.eclipse.leshan.core.util.Hex;
import org.thingsboard.server.cache.ota.OtaPackageDataCache;
import org.thingsboard.server.transport.lwm2m.server.AbstractLwM2mTransportResource;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.FIRMWARE_UPDATE_COAP_RESOURCE;
import static org.thingsboard.server.transport.lwm2m.server.ota.DefaultLwM2MOtaUpdateService.SOFTWARE_UPDATE_COAP_RESOURCE;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.LWM2M_POST_COAP_RESOURCE;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.VALUE_SUB_LWM2M_POST_COAP_RESPONSE;
import static org.thingsboard.server.transport.lwm2m.utils.LwM2MTransportUtil.setValueStr1024;

@Slf4j
public class LwM2mTransportCoapResource extends AbstractLwM2mTransportResource {
    private final ConcurrentMap<String, ObserveRelation> tokenToObserveRelationMap = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, AtomicInteger> tokenToObserveNotificationSeqMap = new ConcurrentHashMap<>();
    private final OtaPackageDataCache otaPackageDataCache;
    private final LwM2mUplinkMsgHandler service;

    public LwM2mTransportCoapResource(OtaPackageDataCache otaPackageDataCache, String name, LwM2mUplinkMsgHandler service) {
        super(name);
        this.otaPackageDataCache = otaPackageDataCache;
        this.setObservable(true); // enable observing
        this.addObserver(new CoapResourceObserver());
        this.service = service;
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
        List<String> uriPath = exchange.getRequestOptions().getUriPath();
        LwM2MCoapRequestPost coapRequest = this.validateParamsRequest(exchange, uriPath);
        if (coapRequest != null) {
            this.sendDataToTransport(coapRequest);
        }
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
                int chunkSize = exchange.getRequestOptions().getBlock2().getSzx();
                boolean lastFlag = otaData.length <= chunkSize;
                response.getOptions().setBlock2(chunkSize, lastFlag, 0);
                log.trace("With block2 Send currentId: [{}], length: [{}], chunkSize [{}], moreFlag [{}]", currentId.toString(), otaData.length, chunkSize, lastFlag);
            } else {
                log.trace("With block1 Send currentId: [{}], length: [{}], ", currentId.toString(), otaData.length);
            }
            exchange.respond(response);
        } else {
            log.trace("Ota packaged currentId: [{}] is not found.", currentId.toString());
        }
    }

    private byte[] getOtaData(UUID currentId) {
        return otaPackageDataCache.get(currentId.toString());
    }

    private void sendDataToTransport(LwM2MCoapRequestPost coapRequest) {
        String payLoadResponse = coapRequest.getLwM2mPath().toString()  + ", " + VALUE_SUB_LWM2M_POST_COAP_RESPONSE;
        if (service.onUpdateResourceValueAfterCoapRequestPost(coapRequest)) {
            payLoadResponse += coapRequest.getValueStr();
            this.sendResponse(coapRequest.getExchange(), CoAP.ResponseCode.CONTENT, payLoadResponse);
        } else {
            payLoadResponse += Hex.encodeHexString(coapRequest.getExchange().advanced().getRequest().getPayload());
            this.sendResponse(coapRequest.getExchange(), CoAP.ResponseCode.UNAUTHORIZED, payLoadResponse);
            log.error(String.format("Invalid registration, endpoint [%s] %s", coapRequest.getEndpoint(), payLoadResponse));
        }
    }

    private void sendResponse(CoapExchange exchange, CoAP.ResponseCode code, String payLoadResponse) {
        Response response = new Response(code);
        response.setPayload(setValueStr1024(payLoadResponse));
        exchange.respond(response);
    }

    /**
     * @param uriPath  [LWM2M_POST_COAP_RESOURCE, {endpoint}, {lwM2mPath}]
     */
    private LwM2MCoapRequestPost validateParamsRequest(CoapExchange exchange, List<String> uriPath) {
        if (uriPath.size() >= 3 && LWM2M_POST_COAP_RESOURCE.equals(uriPath.get(0))) {
            LwM2MCoapRequestPost coapRequest = new LwM2MCoapRequestPost();
            coapRequest.setExchange(exchange);
            coapRequest.setUriPath(uriPath);
            if (coapRequest.validateParamsRequest()) {
                return coapRequest;
            } else {
                this.sendResponse(exchange, CoAP.ResponseCode.BAD_REQUEST, coapRequest.getPayLoadResponse());
            }
        } else {
            String payLoadResponse = String.format("Invalid UriPath. Lwm2m coap Post request: %s", uriPath);
            log.error(payLoadResponse);
            this.sendResponse(exchange, CoAP.ResponseCode.BAD_OPTION, payLoadResponse);
        }
        return null;
    }
}
