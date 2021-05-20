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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.CoapResource;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.function.BiConsumer;

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

    protected void reportActivity(TransportProtos.SessionInfoProto sessionInfo, boolean hasAttributeSubscription, boolean hasRpcSubscription) {
        transportContext.getTransportService().process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                .setAttributeSubscription(hasAttributeSubscription)
                .setRpcSubscription(hasRpcSubscription)
                .setLastActivityTime(System.currentTimeMillis())
                .build(), TransportServiceCallback.EMPTY);
    }

    protected static TransportProtos.SessionEventMsg getSessionEventMsg(TransportProtos.SessionEvent event) {
        return TransportProtos.SessionEventMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .setEvent(event).build();
    }

    public static class CoapDeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponse> {
        private final TransportContext transportContext;
        private final CoapExchange exchange;
        private final BiConsumer<TransportProtos.SessionInfoProto, DeviceProfile> onSuccess;

        public CoapDeviceAuthCallback(TransportContext transportContext, CoapExchange exchange, BiConsumer<TransportProtos.SessionInfoProto, DeviceProfile> onSuccess) {
            this.transportContext = transportContext;
            this.exchange = exchange;
            this.onSuccess = onSuccess;
        }

        @Override
        public void onSuccess(ValidateDeviceCredentialsResponse msg) {
            DeviceProfile deviceProfile = msg.getDeviceProfile();
            if (msg.hasDeviceInfo() && deviceProfile != null) {
                TransportProtos.SessionInfoProto sessionInfoProto = SessionInfoCreator.create(msg, transportContext, UUID.randomUUID());
                onSuccess.accept(sessionInfoProto, deviceProfile);
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

    public static class CoapOkCallback implements TransportServiceCallback<Void> {
        private final CoapExchange exchange;
        private final CoAP.ResponseCode onSuccessResponse;
        private final CoAP.ResponseCode onFailureResponse;

        public CoapOkCallback(CoapExchange exchange, CoAP.ResponseCode onSuccessResponse, CoAP.ResponseCode onFailureResponse) {
            this.exchange = exchange;
            this.onSuccessResponse = onSuccessResponse;
            this.onFailureResponse = onFailureResponse;
        }

        @Override
        public void onSuccess(Void msg) {
            exchange.respond(onSuccessResponse);
        }

        @Override
        public void onError(Throwable e) {
            exchange.respond(onFailureResponse);
        }
    }

    public static class CoapNoOpCallback implements TransportServiceCallback<Void> {
        private final CoapExchange exchange;

        CoapNoOpCallback(CoapExchange exchange) {
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
}
