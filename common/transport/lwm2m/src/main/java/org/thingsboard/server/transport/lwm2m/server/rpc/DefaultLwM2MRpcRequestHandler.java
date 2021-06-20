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
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelAllObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelAllRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MCancelObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDeleteCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDeleteRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDiscoverAllRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDiscoverCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MDiscoverRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MExecuteRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveAllRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MObserveRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MReadRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteAttributesRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteReplaceRequest;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteResponseCallback;
import org.thingsboard.server.transport.lwm2m.server.downlink.TbLwM2MWriteUpdateRequest;
import org.thingsboard.server.transport.lwm2m.server.log.LwM2MTelemetryLogService;
import org.thingsboard.server.transport.lwm2m.server.uplink.LwM2mUplinkMsgHandler;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private final LwM2MTelemetryLogService logService;
    private final Map<UUID, Long> rpcSubscriptions = new ConcurrentHashMap<>();

    @Override
    public void onToDeviceRpcRequest(TransportProtos.ToDeviceRpcRequestMsg rpcRequst, TransportProtos.SessionInfoProto sessionInfo) {
        this.cleanupOldSessions();
        UUID requestUUID = new UUID(rpcRequst.getRequestIdMSB(), rpcRequst.getRequestIdLSB());
        log.warn("Received params: {}", rpcRequst.getParams());
        // We use this map to protect from browser issue that the same command is sent twice.
        // TODO: This is probably not the best place and should be moved to DeviceActor
        if (!this.rpcSubscriptions.containsKey(requestUUID)) {
            LwM2mOperationType operationType = LwM2mOperationType.fromType(rpcRequst.getMethodName());
            if (operationType == null) {
                this.sendErrorRpcResponse(sessionInfo, rpcRequst.getRequestId(), ResponseCode.METHOD_NOT_ALLOWED.getName(), "Unsupported operation type: " + rpcRequst.getMethodName());
                return;
            }
            LwM2mClient client = clientContext.getClientBySessionInfo(sessionInfo);
            if (client.getRegistration() == null) {
                this.sendErrorRpcResponse(sessionInfo, rpcRequst.getRequestId(), ResponseCode.INTERNAL_SERVER_ERROR.getName(), "Registration is empty");
                return;
            }
            try {
                if (operationType.isHasObjectId()) {
                    String objectId = getIdFromParameters(client, rpcRequst);
                    switch (operationType) {
                        case READ:
                            sendReadRequest(client, rpcRequst, objectId);
                            break;
                        case OBSERVE:
                            sendObserveRequest(client, rpcRequst, objectId);
                            break;
                        case DISCOVER:
                            sendDiscoverRequest(client, rpcRequst, objectId);
                            break;
                        case EXECUTE:
                            sendExecuteRequest(client, rpcRequst, objectId);
                            break;
                        case WRITE_ATTRIBUTES:
                            sendWriteAttributesRequest(client, rpcRequst, objectId);
                            break;
                        case OBSERVE_CANCEL:
                            sendCancelObserveRequest(client, rpcRequst, objectId);
                            break;
                        case DELETE:
                            sendDeleteRequest(client, rpcRequst, objectId);
                            break;
                        case WRITE_UPDATE:
                            sendWriteUpdateRequest(client, rpcRequst, objectId);
                            break;
                        case WRITE_REPLACE:
                            sendWriteReplaceRequest(client, rpcRequst, objectId);
                            break;
                        default:
                            throw new IllegalArgumentException("Unsupported operation: " + operationType.name());
                    }
                } else {
                    switch (operationType) {
                        case OBSERVE_CANCEL_ALL:
                            sendCancelAllObserveRequest(client, rpcRequst);
                            break;
                        case OBSERVE_READ_ALL:
                            sendObserveAllRequest(client, rpcRequst);
                            break;
                        case DISCOVER_ALL:
                            sendDiscoverAllRequest(client, rpcRequst);
                            break;
                        case FW_UPDATE:
                            //TODO: implement and add break statement
                        default:
                            throw new IllegalArgumentException("Unsupported operation: " + operationType.name());
                    }
                }
            } catch (IllegalArgumentException e) {
                this.sendErrorRpcResponse(sessionInfo, rpcRequst.getRequestId(), ResponseCode.BAD_REQUEST.getName(), e.getMessage());
            }
        }
    }

    private void sendReadRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MReadRequest request = TbLwM2MReadRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MReadCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcReadResponseCallback<>(transportService, client, requestMsg, versionedId, mainCallback);
        downlinkHandler.sendReadRequest(client, request, rpcCallback);
    }

    private void sendObserveRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MObserveRequest request = TbLwM2MObserveRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MObserveCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcReadResponseCallback<>(transportService, client, requestMsg, versionedId, mainCallback);
        downlinkHandler.sendObserveRequest(client, request, rpcCallback);
    }

    private void sendObserveAllRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg) {
        TbLwM2MObserveAllRequest request = TbLwM2MObserveAllRequest.builder().timeout(this.config.getTimeout()).build();
        downlinkHandler.sendObserveAllRequest(client, request, new RpcLinkSetCallback<>(transportService, client, requestMsg, null));
    }

    private void sendDiscoverAllRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg) {
        TbLwM2MDiscoverAllRequest request = TbLwM2MDiscoverAllRequest.builder().timeout(this.config.getTimeout()).build();
        downlinkHandler.sendDiscoverAllRequest(client, request, new RpcLinkSetCallback<>(transportService, client, requestMsg, null));
    }

    private void sendDiscoverRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MDiscoverRequest request = TbLwM2MDiscoverRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MDiscoverCallback(logService, client, versionedId);
        var rpcCallback = new RpcDiscoverCallback(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendDiscoverRequest(client, request, rpcCallback);
    }

    private void sendExecuteRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MExecuteRequest downlink = TbLwM2MExecuteRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MExecuteCallback(logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendExecuteRequest(client, downlink, rpcCallback);
    }

    private void sendWriteAttributesRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        RpcWriteAttributesRequest requestBody = JacksonUtil.fromString(requestMsg.getParams(), RpcWriteAttributesRequest.class);
        TbLwM2MWriteAttributesRequest request = TbLwM2MWriteAttributesRequest.builder().versionedId(versionedId)
                .attributes(requestBody.getAttributes())
                .timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MWriteAttributesCallback(logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendWriteAttributesRequest(client, request, rpcCallback);
    }

    private void sendWriteUpdateRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        RpcWriteUpdateRequest requestBody = JacksonUtil.fromString(requestMsg.getParams(), RpcWriteUpdateRequest.class);
        TbLwM2MWriteUpdateRequest.TbLwM2MWriteUpdateRequestBuilder builder = TbLwM2MWriteUpdateRequest.builder().versionedId(versionedId);
        builder.value(requestBody.getValue()).timeout(this.config.getTimeout());
        var mainCallback = new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendWriteUpdateRequest(client, builder.build(), rpcCallback);
    }

    private void sendWriteReplaceRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        RpcWriteReplaceRequest requestBody = JacksonUtil.fromString(requestMsg.getParams(), RpcWriteReplaceRequest.class);
        TbLwM2MWriteReplaceRequest request = TbLwM2MWriteReplaceRequest.builder().versionedId(versionedId)
                .value(requestBody.getValue())
                .timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MWriteResponseCallback(uplinkHandler, logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendWriteReplaceRequest(client, request, rpcCallback);
    }

    private void sendCancelObserveRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MCancelObserveRequest downlink = TbLwM2MCancelObserveRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MCancelObserveCallback(logService, client, versionedId);
        var rpcCallback = new RpcCancelObserveCallback(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendCancelObserveRequest(client, downlink, rpcCallback);
    }

    private void sendDeleteRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg, String versionedId) {
        TbLwM2MDeleteRequest downlink = TbLwM2MDeleteRequest.builder().versionedId(versionedId).timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MDeleteCallback(logService, client, versionedId);
        var rpcCallback = new RpcEmptyResponseCallback<>(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendDeleteRequest(client, downlink, rpcCallback);
    }

    private void sendCancelAllObserveRequest(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg requestMsg) {
        TbLwM2MCancelAllRequest downlink = TbLwM2MCancelAllRequest.builder().timeout(this.config.getTimeout()).build();
        var mainCallback = new TbLwM2MCancelAllObserveCallback(logService, client);
        var rpcCallback = new RpcCancelAllObserveCallback(transportService, client, requestMsg, mainCallback);
        downlinkHandler.sendCancelAllRequest(client, downlink, rpcCallback);
    }

    private String getIdFromParameters(LwM2mClient client, TransportProtos.ToDeviceRpcRequestMsg rpcRequst) {
        IdOrKeyRequest requestParams = JacksonUtil.fromString(rpcRequst.getParams(), IdOrKeyRequest.class);
        String targetId;
        if (StringUtils.isNotEmpty(requestParams.getKey())) {
            targetId = clientContext.getObjectIdByKeyNameFromProfile(client, requestParams.getKey());
        } else if (StringUtils.isNotEmpty(requestParams.getId())) {
            targetId = requestParams.getId();
        } else {
            throw new IllegalArgumentException("Can't find 'key' or 'id' in the requestParams parameters!");
        }
        return targetId;
    }

    private void sendErrorRpcResponse(TransportProtos.SessionInfoProto sessionInfo, int requestId, String result, String error) {
        String payload = JacksonUtil.toString(JacksonUtil.newObjectNode().put("result", result).put("error", error));
        TransportProtos.ToDeviceRpcResponseMsg msg = TransportProtos.ToDeviceRpcResponseMsg.newBuilder().setRequestId(requestId).setPayload(payload).build();
        transportService.process(sessionInfo, msg, null);
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

    @Override
    public void onToDeviceRpcResponse(TransportProtos.ToDeviceRpcResponseMsg toDeviceResponse, TransportProtos.SessionInfoProto sessionInfo) {
        log.warn("5) onToDeviceRpcResponse: [{}], sessionUUID: [{}]", toDeviceResponse, new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB()));
        transportService.process(sessionInfo, toDeviceResponse, null);
    }

    public void onToServerRpcResponse(TransportProtos.ToServerRpcResponseMsg toServerResponse) {
        log.info("[{}] toServerRpcResponse", toServerResponse);
    }
}
