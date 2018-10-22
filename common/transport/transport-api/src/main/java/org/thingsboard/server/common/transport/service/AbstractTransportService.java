/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.server.common.transport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 17.10.18.
 */
@Slf4j
public abstract class AbstractTransportService implements TransportService {

    @Value("${transport.rate_limits.enabled}")
    private boolean rateLimitEnabled;
    @Value("${transport.rate_limits.tenant}")
    private String perTenantLimitsConf;
    @Value("${transport.rate_limits.tenant}")
    private String perDevicesLimitsConf;

    protected ScheduledExecutorService schedulerExecutor;
    protected ExecutorService transportCallbackExecutor;
    private ConcurrentMap<UUID, SessionMetaData> sessions = new ConcurrentHashMap<>();

    //TODO: Implement cleanup of this maps.
    private ConcurrentMap<TenantId, TbTransportRateLimits> perTenantLimits = new ConcurrentHashMap<>();
    private ConcurrentMap<DeviceId, TbTransportRateLimits> perDeviceLimits = new ConcurrentHashMap<>();

    @Override
    public void registerAsyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener) {
        sessions.putIfAbsent(toId(sessionInfo), new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listener));
        //TODO: monitor sessions periodically: PING REQ/RESP, etc.
    }

    @Override
    public void registerSyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout) {
        sessions.putIfAbsent(toId(sessionInfo), new SessionMetaData(sessionInfo, TransportProtos.SessionType.SYNC, listener));
        schedulerExecutor.schedule(() -> {
            listener.onRemoteSessionCloseCommand(TransportProtos.SessionCloseNotificationProto.getDefaultInstance());
            deregisterSession(sessionInfo);
        }, timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public void deregisterSession(TransportProtos.SessionInfoProto sessionInfo) {
        sessions.remove(toId(sessionInfo));
    }

    @Override
    public boolean checkLimits(TransportProtos.SessionInfoProto sessionInfo, TransportServiceCallback<Void> callback) {
        if (!rateLimitEnabled) {
            return true;
        }
        TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
        TbTransportRateLimits rateLimits = perTenantLimits.computeIfAbsent(tenantId, id -> new TbTransportRateLimits(perTenantLimitsConf));
        if (!rateLimits.tryConsume()) {
            if (callback != null) {
                callback.onError(new TbRateLimitsException(EntityType.TENANT));
            }
            return false;
        }
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
        rateLimits = perDeviceLimits.computeIfAbsent(deviceId, id -> new TbTransportRateLimits(perDevicesLimitsConf));
        if (!rateLimits.tryConsume()) {
            if (callback != null) {
                callback.onError(new TbRateLimitsException(EntityType.DEVICE));
            }
            return false;
        }
        return true;
    }

    protected void processToTransportMsg(TransportProtos.DeviceActorToTransportMsg toSessionMsg) {
        UUID sessionId = new UUID(toSessionMsg.getSessionIdMSB(), toSessionMsg.getSessionIdLSB());
        SessionMetaData md = sessions.get(sessionId);
        if (md != null) {
            SessionMsgListener listener = md.getListener();
            transportCallbackExecutor.submit(() -> {
                if (toSessionMsg.hasGetAttributesResponse()) {
                    listener.onGetAttributesResponse(toSessionMsg.getGetAttributesResponse());
                }
                if (toSessionMsg.hasAttributeUpdateNotification()) {
                    listener.onAttributeUpdate(toSessionMsg.getAttributeUpdateNotification());
                }
                if (toSessionMsg.hasSessionCloseNotification()) {
                    listener.onRemoteSessionCloseCommand(toSessionMsg.getSessionCloseNotification());
                }
                if (toSessionMsg.hasToDeviceRequest()) {
                    listener.onToDeviceRpcRequest(toSessionMsg.getToDeviceRequest());
                }
                if (toSessionMsg.hasToServerResponse()) {
                    listener.onToServerRpcResponse(toSessionMsg.getToServerResponse());
                }
            });
            if (md.getSessionType() == TransportProtos.SessionType.SYNC) {
                deregisterSession(md.getSessionInfo());
            }
        } else {
            //TODO: should we notify the device actor about missed session?
            log.debug("[{}] Missing session.", sessionId);
        }
    }

    protected UUID toId(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    String getRoutingKey(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()).toString();
    }

    public void init() {
        if (rateLimitEnabled) {
            //Just checking the configuration parameters
            new TbTransportRateLimits(perTenantLimitsConf);
            new TbTransportRateLimits(perDevicesLimitsConf);
        }
        this.schedulerExecutor = Executors.newSingleThreadScheduledExecutor();
        this.transportCallbackExecutor = new ThreadPoolExecutor(0, 20, 60L, TimeUnit.SECONDS, new SynchronousQueue<>());
    }

    public void destroy() {
        if (rateLimitEnabled) {
            perTenantLimits.clear();
            perDeviceLimits.clear();
        }
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdownNow();
        }
        if (transportCallbackExecutor != null) {
            transportCallbackExecutor.shutdownNow();
        }
    }
}
