/**
 * Copyright © 2016-2018 The Thingsboard Authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.transport.http;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportContext;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.adaptor.JsonConverter;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.*;
import org.thingsboard.server.transport.http.session.HttpSessionCtx;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author Andrew Shvayka
 */
@RestController
@ConditionalOnProperty(prefix = "transport.http", value = "enabled", havingValue = "true", matchIfMissing = true)
@RequestMapping("/api/v1")
@Slf4j
public class DeviceApiController {

    @Autowired
    private HttpTransportContext transportContext;

    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> getDeviceAttributes(@PathVariable("deviceToken") String deviceToken,
                                                              @RequestParam(value = "clientKeys", required = false, defaultValue = "") String clientKeys,
                                                              @RequestParam(value = "sharedKeys", required = false, defaultValue = "") String sharedKeys,
                                                              HttpServletRequest httpRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<>();
        if (quotaExceeded(httpRequest, responseWriter)) {
            return responseWriter;
        }
        transportContext.getTransportService().process(ValidateDeviceTokenRequestMsg.newBuilder().setToken(deviceToken).build(),
                new DeviceAuthCallback(transportContext, responseWriter, sessionInfo -> {
                    GetAttributeRequestMsg.Builder request = GetAttributeRequestMsg.newBuilder().setRequestId(0);
                    List<String> clientKeySet = !StringUtils.isEmpty(clientKeys) ? Arrays.asList(clientKeys.split(",")) : null;
                    List<String> sharedKeySet = !StringUtils.isEmpty(sharedKeys) ? Arrays.asList(sharedKeys.split(",")) : null;
                    if (clientKeySet != null) {
                        request.addAllClientAttributeNames(clientKeySet);
                    }
                    if (sharedKeySet != null) {
                        request.addAllSharedAttributeNames(sharedKeySet);
                    }
                    TransportService transportService = transportContext.getTransportService();
                    transportService.registerSession(sessionInfo, TransportProtos.SessionType.SYNC, new HttpSessionListener(transportContext, responseWriter));
                    transportService.process(sessionInfo, request.build(), new SessionCloseOnErrorCallback(transportService, sessionInfo));
                }));
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/attributes", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postDeviceAttributes(@PathVariable("deviceToken") String deviceToken,
                                                               @RequestBody String json, HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        if (quotaExceeded(request, responseWriter)) {
            return responseWriter;
        }
//        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
//        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
//            try {
//                process(ctx, JsonConverter.convertToAttributes(new JsonParser().parse(json)));
//            } catch (IllegalStateException | JsonSyntaxException ex) {
//                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
//            }
//        } else {
//            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
//        }
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/telemetry", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postTelemetry(@PathVariable("deviceToken") String deviceToken,
                                                        @RequestBody String json, HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        if (quotaExceeded(request, responseWriter)) {
            return responseWriter;
        }
        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
//        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
//            try {
//                process(ctx, JsonConverter.convertToTelemetry(new JsonParser().parse(json)));
//            } catch (IllegalStateException | JsonSyntaxException ex) {
//                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
//            }
//        } else {
//            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
//        }
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToCommands(@PathVariable("deviceToken") String deviceToken,
                                                              @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout,
                                                              HttpServletRequest request) {

//        return subscribe(deviceToken, timeout, new RpcSubscribeMsg(), request);
        return null;
    }

    @RequestMapping(value = "/{deviceToken}/rpc/{requestId}", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> replyToCommand(@PathVariable("deviceToken") String deviceToken,
                                                         @PathVariable("requestId") Integer requestId,
                                                         @RequestBody String json, HttpServletRequest request) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        if (quotaExceeded(request, responseWriter)) {
            return responseWriter;
        }
//        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
//        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
//            try {
//                JsonObject response = new JsonParser().parse(json).getAsJsonObject();
//                process(ctx, new ToDeviceRpcResponseMsg(requestId, response.toString()));
//            } catch (IllegalStateException | JsonSyntaxException ex) {
//                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
//            }
//        } else {
//            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
//        }
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/rpc", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity> postRpcRequest(@PathVariable("deviceToken") String deviceToken,
                                                         @RequestBody String json, HttpServletRequest httpRequest) {
        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
        if (quotaExceeded(httpRequest, responseWriter)) {
            return responseWriter;
        }
        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter);
//        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
//            try {
//                JsonObject request = new JsonParser().parse(json).getAsJsonObject();
//                process(ctx, new ToServerRpcRequestMsg(0,
//                        request.get("method").getAsString(),
//                        request.get("params").toString()));
//            } catch (IllegalStateException | JsonSyntaxException ex) {
//                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
//            }
//        } else {
//            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
//        }
        return responseWriter;
    }

    @RequestMapping(value = "/{deviceToken}/attributes/updates", method = RequestMethod.GET, produces = "application/json")
    public DeferredResult<ResponseEntity> subscribeToAttributes(@PathVariable("deviceToken") String deviceToken,
                                                                @RequestParam(value = "timeout", required = false, defaultValue = "0") long timeout,
                                                                HttpServletRequest httpRequest) {
        return null;
//        return subscribe(deviceToken, timeout, new AttributesSubscribeMsg(), httpRequest);
    }

//    private DeferredResult<ResponseEntity> subscribe(String deviceToken, long timeout, FromDeviceMsg msg, HttpServletRequest httpRequest) {
//        DeferredResult<ResponseEntity> responseWriter = new DeferredResult<ResponseEntity>();
//        if (quotaExceeded(httpRequest, responseWriter)) {
//            return responseWriter;
//        }
//        HttpSessionCtx ctx = getHttpSessionCtx(responseWriter, timeout);
//        if (ctx.login(new DeviceTokenCredentials(deviceToken))) {
//            try {
//                process(ctx, msg);
//            } catch (IllegalStateException | JsonSyntaxException ex) {
//                responseWriter.setResult(new ResponseEntity<>(HttpStatus.BAD_REQUEST));
//            }
//        } else {
//            responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
//        }
//        return responseWriter;
//    }

    private HttpSessionCtx getHttpSessionCtx(DeferredResult<ResponseEntity> responseWriter) {
        return getHttpSessionCtx(responseWriter, transportContext.getDefaultTimeout());
    }

    private HttpSessionCtx getHttpSessionCtx(DeferredResult<ResponseEntity> responseWriter, long timeout) {
        return null;
//        return new HttpSessionCtx(processor, authService, responseWriter, timeout != 0 ? timeout : defaultTimeout);
    }

//    private void process(HttpSessionCtx ctx, FromDeviceMsg request) {
//        AdaptorToSessionActorMsg msg = new BasicAdaptorToSessionActorMsg(ctx, request);
////        processor.process(new BasicTransportToDeviceSessionActorMsg(ctx.getDevice(), msg));
//    }

    private boolean quotaExceeded(HttpServletRequest request, DeferredResult<ResponseEntity> responseWriter) {
        if (transportContext.getQuotaService().isQuotaExceeded(request.getRemoteAddr())) {
            log.warn("REST Quota exceeded for [{}] . Disconnect", request.getRemoteAddr());
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED));
            return true;
        }
        return false;
    }

    private static class DeviceAuthCallback implements TransportServiceCallback<ValidateDeviceCredentialsResponseMsg> {
        private final TransportContext transportContext;
        private final DeferredResult<ResponseEntity> responseWriter;
        private final Consumer<SessionInfoProto> onSuccess;

        DeviceAuthCallback(TransportContext transportContext, DeferredResult<ResponseEntity> responseWriter, Consumer<SessionInfoProto> onSuccess) {
            this.transportContext = transportContext;
            this.responseWriter = responseWriter;
            this.onSuccess = onSuccess;
        }

        @Override
        public void onSuccess(ValidateDeviceCredentialsResponseMsg msg) {
            if (msg.hasDeviceInfo()) {
                UUID sessionId = UUID.randomUUID();
                DeviceInfoProto deviceInfoProto = msg.getDeviceInfo();
                SessionInfoProto sessionInfo = SessionInfoProto.newBuilder()
                        .setNodeId(transportContext.getNodeId())
                        .setTenantIdMSB(deviceInfoProto.getTenantIdMSB())
                        .setTenantIdLSB(deviceInfoProto.getTenantIdLSB())
                        .setDeviceIdMSB(deviceInfoProto.getDeviceIdMSB())
                        .setDeviceIdLSB(deviceInfoProto.getDeviceIdLSB())
                        .setSessionIdMSB(sessionId.getMostSignificantBits())
                        .setSessionIdLSB(sessionId.getLeastSignificantBits())
                        .build();
                onSuccess.accept(sessionInfo);
            } else {
                responseWriter.setResult(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
            }
        }

        @Override
        public void onError(Throwable e) {
            log.warn("Failed to process request", e);
            responseWriter.setResult(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }
    }

    private static class SessionCloseOnErrorCallback implements TransportServiceCallback<Void> {
        private final TransportService transportService;
        private final SessionInfoProto sessionInfo;

        SessionCloseOnErrorCallback(TransportService transportService, SessionInfoProto sessionInfo) {
            this.transportService = transportService;
            this.sessionInfo = sessionInfo;
        }

        @Override
        public void onSuccess(Void msg) {

        }

        @Override
        public void onError(Throwable e) {
            transportService.deregisterSession(sessionInfo);
        }
    }

    private static class HttpSessionListener implements SessionMsgListener {

        private final TransportContext transportContext;
        private final DeferredResult<ResponseEntity> responseWriter;

        HttpSessionListener(TransportContext transportContext, DeferredResult<ResponseEntity> responseWriter) {
            this.transportContext = transportContext;
            this.responseWriter = responseWriter;
        }

        @Override
        public void onGetAttributesResponse(GetAttributeResponseMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        @Override
        public void onAttributeUpdate(AttributeUpdateNotificationMsg msg) {
            responseWriter.setResult(new ResponseEntity<>(JsonConverter.toJson(msg).toString(), HttpStatus.OK));
        }

        @Override
        public void onRemoteSessionCloseCommand(SessionCloseNotificationProto sessionCloseNotification) {
        }

        @Override
        public void onToDeviceRpcRequest(ToDeviceRpcRequestMsg toDeviceRequest) {

        }

        @Override
        public void onToServerRpcResponse(ToServerRpcResponseMsg toServerResponse) {

        }
    }
}
