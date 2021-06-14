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
package org.thingsboard.server.transport.lwm2m.server.rpc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.leshan.core.ResponseCode;
import org.eclipse.leshan.server.registration.Registration;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbLwM2mTransportComponent;
import org.thingsboard.server.transport.lwm2m.config.LwM2MTransportServerConfig;
import org.thingsboard.server.transport.lwm2m.server.LwM2mOperationType;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClient;
import org.thingsboard.server.transport.lwm2m.server.client.LwM2mClientContext;
import org.thingsboard.server.transport.lwm2m.server.downlink.LwM2mDownlinkMsgHandler;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadRequest;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.eclipse.californium.core.coap.CoAP.ResponseCode.BAD_REQUEST;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_ERROR;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_INFO;
import static org.thingsboard.server.transport.lwm2m.server.LwM2mTransportUtil.LOG_LW2M_VALUE;

@Slf4j
@Service
@TbLwM2mTransportComponent
@RequiredArgsConstructor
public class DefaultLwM2MRpcRequestHandler implements LwM2MRpcRequestHandler {

    private final TransportService transportService;
    private final LwM2mClientContext clientContext;
    private final LwM2MTransportServerConfig config;
    private final LwM2mUplinkMsgHandler uplinkHandler;
    private final LwM2mDownlinkMsgHandler downlinkHandler;
    private final Map<UUID, Long> rpcSubscriptions = new ConcurrentHashMap<>();


    @Override
    public void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg rpcRequst, TransportProtos.SessionInfoProto sessionInfo) {
        this.cleanupOldSessions();
        UUID requestUUID = new UUID(rpcRequst.getRequestIdMSB(), rpcRequst.getRequestIdLSB());
        log.warn("Received params: {}", rpcRequst.getParams());
        // TODO: We use this map to protect from browser issue that the same command is sent twice. This is probably not the best place and should be moved to DeviceActor
        if (!this.rpcSubscriptions.containsKey(requestUUID)) {
            LwM2mOperationType operationType = LwM2mOperationType.fromType(rpcRequst.getMethodName());
            if (operationType == null) {
                this.sendErrorRpcResponse(sessionInfo, rpcRequst.getRequestId(), ResponseCode.METHOD_NOT_ALLOWED.getName(), "Unsupported operation type: " + rpcRequst.getMethodName());
            }
            LwM2mClient client = clientContext.getClientBySessionInfo(sessionInfo);
            if (client.getRegistration() == null) {
                this.sendErrorRpcResponse(sessionInfo, rpcRequst.getRequestId(), ResponseCode.INTERNAL_SERVER_ERROR.getName(), "Registration is empty");
            }
            switch (operationType) {
                case READ:
                    sendReadRequest(client, rpcRequst);
                    break;
            }


//            log.warn("4) rpcRequst: [{}], sessionUUID: [{}]", rpcRequst, new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
//            String bodyParams = StringUtils.trimToNull(rpcRequst.getParams()) != null ? rpcRequst.getParams() : "null";
//            LwM2mOperationType lwM2mTypeOper = setValidTypeOper(rpcRequst.getMethodName());
//            this.rpcSubscriptions.put(requestUUID, rpcRequst.getExpirationTime());
//            LwM2mClientRpcRequest lwm2mClientRpcRequest = null;
//            try {
//                LwM2mClient client = clientContext.getClientBySessionInfo(sessionInfo);
//                Registration registration = client.getRegistration();
//                if (registration != null) {
//                    lwm2mClientRpcRequest = new LwM2mClientRpcRequest(lwM2mTypeOper, bodyParams, rpcRequst.getRequestId(), sessionInfo, registration, uplinkHandler);
//                    if (lwm2mClientRpcRequest.getErrorMsg() != null) {
//                        lwm2mClientRpcRequest.setResponseCode(BAD_REQUEST.name());
//                        this.onToDeviceRpcResponse(lwm2mClientRpcRequest.getDeviceRpcResponseResultMsg(), sessionInfo);
//                    } else {
//                        //TODO: use different methods and RPC callback wrapper.
////                        defaultLwM2MDownlinkMsgHandler.sendAllRequest(client, lwm2mClientRpcRequest.getTargetIdVer(), lwm2mClientRpcRequest.getTypeOper(),
////                                null,
////                                lwm2mClientRpcRequest.getValue() == null ? lwm2mClientRpcRequest.getParams() : lwm2mClientRpcRequest.getValue(),
////                                this.config.getTimeout(), lwm2mClientRpcRequest);
//                    }
//                } else {
//                    this.sendErrorRpcResponse(lwm2mClientRpcRequest, "registration == null", sessionInfo);
//                }
//            } catch (Exception e) {
//                this.sendErrorRpcResponse(lwm2mClientRpcRequest, e.getMessage(), sessionInfo);
//            }
        }
    }

    private void sendReadRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg rpcRequst) {
        String id = getIdFromParameters(rpcRequst);
        TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(id).timeout(this.config.getTimeout()).build();
        downlinkHandler.sendReadRequest(client, request, new TbLwM2MReadCallback(uplinkHandler, client, id));
    }

    private String getIdFromParameters(TransportProtos.ToDeviceRpcRequestMsg rpcRequst) {
        IdOrKeyRequest requestParams = JacksonUtil.fromString(rpcRequst.getParams(), IdOrKeyRequest.class);
        String id;
        if (StringUtils.isNotEmpty(requestParams.getKey())) {
            id = requestParams.getKey();
        } else if (StringUtils.isNotEmpty(requestParams.getId())) {
            id = requestParams.getId();
        } else {
            throw new IllegalArgumentException("Can't find 'key' or 'id' in the requestParams parameters!");
        }
        return id;
    }

    private String getTargetId(LwM2mClient client, String params) {

    }

    private void sendErrorRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, String result, String error) {
        String payload = JacksonUtil.toString(JacksonUtil.newObjectNode().put("result", result).put("error", error));
        TransportProtos.ToDeviceRpcResponseMsg msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(payload).build();
        transportService.process(sessionInfo, msg, null);
    }

    private void sendErrorRpcResponse(LwM2mClientRpcRequest lwm2mClientRpcRequest, String msgError, TransportProtos.SessionInfoProto sessionInfo) {
        if (lwm2mClientRpcRequest == null) {
            lwm2mClientRpcRequest = new LwM2mClientRpcRequest();
        }
        lwm2mClientRpcRequest.setResponseCode(BAD_REQUEST.name());
        if (lwm2mClientRpcRequest.getErrorMsg() == null) {
            lwm2mClientRpcRequest.setErrorMsg(msgError);
        }
        this.onToDeviceRpcResponse(lwm2mClientRpcRequest.getDeviceRpcResponseResultMsg(), sessionInfo);
    }

    private void cleanupOldSessions() {
        log.warn("4.1) before rpcSubscriptions.size(): [{}]", rpcSubscriptions.size());
        if (rpcSubscriptions.size() > 0) {
            long currentTime = System.currentTimeMillis();
            Set<UUID> rpcSubscriptionsToRemove = rpcSubscriptions.entrySet().stream().filter(kv -> currentTime > kv.getValue()).map(Map.Entry::getKey).collect(Collectors.toSet());
            log.warn("4.2) System.currentTimeMillis(): [{}]", System.currentTimeMillis());
            log.warn("4.3) rpcSubscriptionsToRemove: [{}]", rpcSubscriptionsToRemove);
            rpcSubscriptionsToRemove.forEach(rpcSubscriptions::remove);
        }
        log.warn("4.4) after rpcSubscriptions.size(): [{}]", rpcSubscriptions.size());
    }

    public void sentRpcResponse(LwM2mClientRpcRequest rpcRequest, String requestCode, String msg, String typeMsg) {
        rpcRequest.setResponseCode(requestCode);
        if (LOG_LW2M_ERROR.equals(typeMsg)) {
            rpcRequest.setInfoMsg(null);
            rpcRequest.setValueMsg(null);
            if (rpcRequest.getErrorMsg() == null) {
                msg = msg.isEmpty() ? null : msg;
                rpcRequest.setErrorMsg(msg);
            }
        } else if (LOG_LW2M_INFO.equals(typeMsg)) {
            if (rpcRequest.getInfoMsg() == null) {
                rpcRequest.setInfoMsg(msg);
            }
        } else if (LOG_LW2M_VALUE.equals(typeMsg)) {
            if (rpcRequest.getValueMsg() == null) {
                rpcRequest.setValueMsg(msg);
            }
        }
        this.onToDeviceRpcResponse(rpcRequest.getDeviceRpcResponseResultMsg(), rpcRequest.getSessionInfo());
    }

    @Override
    public void onToDeviceRpcResponse(TransportProtos.ToDeviceRpcResponseMsg toDeviceResponse, TransportProtos.SessionInfoProto sessionInfo) {
        log.warn("5) onToDeviceRpcResponse: [{}], sessionUUID: [{}]", toDeviceResponse, new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
        transportService.process(sessionInfo, toDeviceResponse, null);
    }

    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        log.info("[{}] toServerRpcResponse", toServerResponse);
    }
}
