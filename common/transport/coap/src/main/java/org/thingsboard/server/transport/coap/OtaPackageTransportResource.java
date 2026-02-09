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
package org.thingsboard.server.transport.coap;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.californium.core.coap.CoAP;
import org.eclipse.californium.core.coap.Request;
import org.eclipse.californium.core.coap.Response;
import org.eclipse.californium.core.network.Exchange;
import org.eclipse.californium.core.server.resources.CoapExchange;
import org.eclipse.californium.core.server.resources.Resource;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.ota.OtaPackageType;
import org.thingsboard.server.common.data.security.DeviceTokenCredentials;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.SessionInfoCreator;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.transport.coap.callback.CoapDeviceAuthCallback;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class OtaPackageTransportResource extends AbstractCoapTransportResource {
    private static final int ACCESS_TOKEN_POSITION = 2;

    private final OtaPackageType otaPackageType;

    public OtaPackageTransportResource(CoapTransportContext ctx, OtaPackageType otaPackageType) {
        super(ctx, otaPackageType.getKeyPrefix());
        this.otaPackageType = otaPackageType;

        this.setObservable(true);
    }

    @Override
    protected void processHandleGet(CoapExchange exchange) {
        log.trace("Processing {}", exchange.advanced().getRequest());
        exchange.accept();
        Exchange advanced = exchange.advanced();
        Request request = advanced.getRequest();
        processAccessTokenRequest(exchange, request);
    }

    @Override
    protected void processHandlePost(CoapExchange exchange) {
        exchange.respond(CoAP.ResponseCode.METHOD_NOT_ALLOWED);
    }

    private void processAccessTokenRequest(CoapExchange exchange, Request request) {
        Optional<DeviceTokenCredentials> credentials = decodeCredentials(request);
        if (credentials.isEmpty()) {
            exchange.respond(CoAP.ResponseCode.UNAUTHORIZED);
            return;
        }
        transportService.process(DeviceTransportType.COAP, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(credentials.get().getCredentialsId()).build(),
                new CoapDeviceAuthCallback(exchange, (msg, deviceProfile) -> {
                    getOtaPackageCallback(msg, exchange, otaPackageType);
                }));
    }

    private void getOtaPackageCallback(ValidateDeviceCredentialsResponse msg, CoapExchange exchange, OtaPackageType firmwareType) {
        TenantId tenantId = msg.getDeviceInfo().getTenantId();
        DeviceId deviceId = msg.getDeviceInfo().getDeviceId();
        TransportProtos.GetOtaPackageRequestMsg requestMsg = TransportProtos.GetOtaPackageRequestMsg.newBuilder()
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits())
                .setDeviceIdMSB(deviceId.getId().getMostSignificantBits())
                .setDeviceIdLSB(deviceId.getId().getLeastSignificantBits())
                .setType(firmwareType.name()).build();
        transportContext.getTransportService().process(SessionInfoCreator.create(msg, transportContext, UUID.randomUUID()), requestMsg, new OtaPackageCallback(exchange));
    }

    private Optional<DeviceTokenCredentials> decodeCredentials(Request request) {
        List<String> uriPath = request.getOptions().getUriPath();
        if (uriPath.size() == ACCESS_TOKEN_POSITION) {
            return Optional.of(new DeviceTokenCredentials(uriPath.get(ACCESS_TOKEN_POSITION - 1)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Resource getChild(String name) {
        return this;
    }

    private class OtaPackageCallback implements TransportServiceCallback<TransportProtos.GetOtaPackageResponseMsg> {
        private final CoapExchange exchange;

        OtaPackageCallback(CoapExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public void onSuccess(TransportProtos.GetOtaPackageResponseMsg msg) {
            String title = exchange.getQueryParameter("title");
            String version = exchange.getQueryParameter("version");
            if (msg.getResponseStatus().equals(TransportProtos.ResponseStatus.SUCCESS)) {
                String firmwareId = new UUID(msg.getOtaPackageIdMSB(), msg.getOtaPackageIdLSB()).toString();
                if ((title == null || msg.getTitle().equals(title)) && (version == null || msg.getVersion().equals(version))) {
                    String strChunkSize = exchange.getQueryParameter("size");
                    String strChunk = exchange.getQueryParameter("chunk");
                    int chunkSize = StringUtils.isEmpty(strChunkSize) ? 0 : Integer.parseInt(strChunkSize);
                    int chunk = StringUtils.isEmpty(strChunk) ? 0 : Integer.parseInt(strChunk);
                    respondOtaPackage(exchange, transportContext.getOtaPackageDataCache().get(firmwareId, chunkSize, chunk));
                } else {
                    exchange.respond(CoAP.ResponseCode.BAD_REQUEST);
                }
            } else {
                exchange.respond(CoAP.ResponseCode.NOT_FOUND);
            }
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            exchange.respond(CoAP.ResponseCode.INTERNAL_SERVER_ERROR);
        }
    }

    private void respondOtaPackage(CoapExchange exchange, byte[] data) {
        Response response = new Response(CoAP.ResponseCode.CONTENT);
        if (data != null && data.length > 0) {
            response.setPayload(data);
            if (exchange.getRequestOptions().getBlock2() != null) {
                int chunkSize = exchange.getRequestOptions().getBlock2().getSzx();
                int blockNum = exchange.getRequestOptions().getBlock2().getNum();
                boolean lastFlag = data.length <= chunkSize;
                response.getOptions().setBlock2(chunkSize, lastFlag, blockNum);
            }
            transportContext.getExecutor().submit(() -> exchange.respond(response));
        }
    }

}
