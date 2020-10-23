/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.protobuf.ByteString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.TenantProfileId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.msg.queue.ServiceQueue;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.common.msg.tools.TbRateLimitsException;
import org.thingsboard.server.common.stats.MessagesStats;
import org.thingsboard.server.common.stats.StatsFactory;
import org.thingsboard.server.common.stats.StatsType;
import org.thingsboard.server.common.transport.SessionMsgListener;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportService;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.TransportTenantProfileCache;
import org.thingsboard.server.common.transport.auth.GetOrCreateDeviceFromGatewayResponse;
import org.thingsboard.server.common.transport.auth.TransportDeviceInfo;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.limits.TransportRateLimitService;
import org.thingsboard.server.common.transport.util.DataDecodingEncodingService;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.EntityDeleteMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ProvisionDeviceResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.ToCoreMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToRuleEngineMsg;
import org.thingsboard.server.gen.transport.TransportProtos.ToTransportMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiRequestMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportApiResponseMsg;
import org.thingsboard.server.gen.transport.TransportProtos.TransportToDeviceActorMsg;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueConsumer;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.AsyncCallbackTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.provider.TbTransportQueueFactory;
import org.thingsboard.server.queue.scheduler.SchedulerComponent;
import org.thingsboard.server.queue.usagestats.TbUsageStatsClient;
import org.thingsboard.server.queue.util.TbTransportComponent;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ashvayka on 17.10.18.
 */
@Slf4j
@Service
@TbTransportComponent
public class DefaultTransportService implements TransportService {

    @Value("${transport.sessions.inactivity_timeout}")
    private long sessionInactivityTimeout;
    @Value("${transport.sessions.report_timeout}")
    private long sessionReportTimeout;
    @Value("${transport.client_side_rpc.timeout:60000}")
    private long clientSideRpcTimeout;
    @Value("${queue.transport.poll_interval}")
    private int notificationsPollDuration;

    private final Gson gson = new Gson();
    private final TbTransportQueueFactory queueProvider;
    private final TbQueueProducerProvider producerProvider;
    private final PartitionService partitionService;
    private final TbServiceInfoProvider serviceInfoProvider;
    private final StatsFactory statsFactory;
    private final TransportDeviceProfileCache deviceProfileCache;
    private final TransportTenantProfileCache tenantProfileCache;
    private final TbUsageStatsClient apiUsageStatsClient;
    private final TransportRateLimitService rateLimitService;
    private final DataDecodingEncodingService dataDecodingEncodingService;
    private final SchedulerComponent scheduler;

    protected TbQueueRequestTemplate<TbProtoQueueMsg<TransportApiRequestMsg>, TbProtoQueueMsg<TransportApiResponseMsg>> transportApiRequestTemplate;
    protected TbQueueProducer<TbProtoQueueMsg<ToRuleEngineMsg>> ruleEngineMsgProducer;
    protected TbQueueProducer<TbProtoQueueMsg<ToCoreMsg>> tbCoreMsgProducer;
    protected TbQueueConsumer<TbProtoQueueMsg<ToTransportMsg>> transportNotificationsConsumer;

    protected MessagesStats ruleEngineProducerStats;
    protected MessagesStats tbCoreProducerStats;
    protected MessagesStats transportApiStats;

    protected ExecutorService transportCallbackExecutor;
    private ExecutorService mainConsumerExecutor;

    private final ConcurrentMap<UUID, SessionMetaData> sessions = new ConcurrentHashMap<>();
    private final Map<String, RpcRequestMetadata> toServerRpcPendingMap = new ConcurrentHashMap<>();

    private volatile boolean stopped = false;

    public DefaultTransportService(TbServiceInfoProvider serviceInfoProvider,
                                   TbTransportQueueFactory queueProvider,
                                   TbQueueProducerProvider producerProvider,
                                   PartitionService partitionService,
                                   StatsFactory statsFactory,
                                   TransportDeviceProfileCache deviceProfileCache,
                                   TransportTenantProfileCache tenantProfileCache,
                                   TbUsageStatsClient apiUsageStatsClient, TransportRateLimitService rateLimitService,
                                   DataDecodingEncodingService dataDecodingEncodingService, SchedulerComponent scheduler) {
        this.serviceInfoProvider = serviceInfoProvider;
        this.queueProvider = queueProvider;
        this.producerProvider = producerProvider;
        this.partitionService = partitionService;
        this.statsFactory = statsFactory;
        this.deviceProfileCache = deviceProfileCache;
        this.tenantProfileCache = tenantProfileCache;
        this.apiUsageStatsClient = apiUsageStatsClient;
        this.rateLimitService = rateLimitService;
        this.dataDecodingEncodingService = dataDecodingEncodingService;
        this.scheduler = scheduler;
    }

    @PostConstruct
    public void init() {
        this.ruleEngineProducerStats = statsFactory.createMessagesStats(StatsType.RULE_ENGINE.getName() + ".producer");
        this.tbCoreProducerStats = statsFactory.createMessagesStats(StatsType.CORE.getName() + ".producer");
        this.transportApiStats = statsFactory.createMessagesStats(StatsType.TRANSPORT.getName() + ".producer");
        this.transportCallbackExecutor = Executors.newWorkStealingPool(20);
        this.scheduler.scheduleAtFixedRate(this::checkInactivityAndReportActivity, new Random().nextInt((int) sessionReportTimeout), sessionReportTimeout, TimeUnit.MILLISECONDS);
        transportApiRequestTemplate = queueProvider.createTransportApiRequestTemplate();
        transportApiRequestTemplate.setMessagesStats(transportApiStats);
        ruleEngineMsgProducer = producerProvider.getRuleEngineMsgProducer();
        tbCoreMsgProducer = producerProvider.getTbCoreMsgProducer();
        transportNotificationsConsumer = queueProvider.createTransportNotificationsConsumer();
        TopicPartitionInfo tpi = partitionService.getNotificationsTopic(ServiceType.TB_TRANSPORT, serviceInfoProvider.getServiceId());
        transportNotificationsConsumer.subscribe(Collections.singleton(tpi));
        transportApiRequestTemplate.init();
        mainConsumerExecutor = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("transport-consumer"));
        mainConsumerExecutor.execute(() -> {
            while (!stopped) {
                try {
                    List<TbProtoQueueMsg<ToTransportMsg>> records = transportNotificationsConsumer.poll(notificationsPollDuration);
                    if (records.size() == 0) {
                        continue;
                    }
                    records.forEach(record -> {
                        try {
                            processToTransportMsg(record.getValue());
                        } catch (Throwable e) {
                            log.warn("Failed to process the notification.", e);
                        }
                    });
                    transportNotificationsConsumer.commit();
                } catch (Exception e) {
                    if (!stopped) {
                        log.warn("Failed to obtain messages from queue.", e);
                        try {
                            Thread.sleep(notificationsPollDuration);
                        } catch (InterruptedException e2) {
                            log.trace("Failed to wait until the server has capacity to handle new requests", e2);
                        }
                    }
                }
            }
        });
    }

    @PreDestroy
    public void destroy() {
        stopped = true;

        if (transportNotificationsConsumer != null) {
            transportNotificationsConsumer.unsubscribe();
        }
        if (transportCallbackExecutor != null) {
            transportCallbackExecutor.shutdownNow();
        }
        if (mainConsumerExecutor != null) {
            mainConsumerExecutor.shutdownNow();
        }
        if (transportApiRequestTemplate != null) {
            transportApiRequestTemplate.stop();
        }
    }

    @Override
    public void registerAsyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener) {
        sessions.putIfAbsent(toSessionId(sessionInfo), new SessionMetaData(sessionInfo, TransportProtos.SessionType.ASYNC, listener));
    }

    @Override
    public TransportProtos.GetEntityProfileResponseMsg getEntityProfile(TransportProtos.GetEntityProfileRequestMsg msg) {
        TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg> protoMsg =
                new TbProtoQueueMsg<>(UUID.randomUUID(), TransportProtos.TransportApiRequestMsg.newBuilder().setEntityProfileRequestMsg(msg).build());
        try {
            TbProtoQueueMsg<TransportApiResponseMsg> response = transportApiRequestTemplate.send(protoMsg).get();
            return response.getValue().getEntityProfileResponseMsg();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void process(DeviceTransportType transportType, TransportProtos.ValidateDeviceTokenRequestMsg msg,
                        TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        log.trace("Processing msg: {}", msg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(),
                TransportApiRequestMsg.newBuilder().setValidateTokenRequestMsg(msg).build());
        doProcess(transportType, protoMsg, callback);
    }

    @Override
    public void process(DeviceTransportType transportType, TransportProtos.ValidateBasicMqttCredRequestMsg msg,
                        TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        log.trace("Processing msg: {}", msg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(),
                TransportApiRequestMsg.newBuilder().setValidateBasicMqttCredRequestMsg(msg).build());
        doProcess(transportType, protoMsg, callback);
    }

    @Override
    public void process(DeviceTransportType transportType, TransportProtos.ValidateDeviceX509CertRequestMsg msg, TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        log.trace("Processing msg: {}", msg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setValidateX509CertRequestMsg(msg).build());
        doProcess(transportType, protoMsg, callback);
    }

    private void doProcess(DeviceTransportType transportType, TbProtoQueueMsg<TransportApiRequestMsg> protoMsg,
                           TransportServiceCallback<ValidateDeviceCredentialsResponse> callback) {
        ListenableFuture<ValidateDeviceCredentialsResponse> response = Futures.transform(transportApiRequestTemplate.send(protoMsg), tmp -> {
            TransportProtos.ValidateDeviceCredentialsResponseMsg msg = tmp.getValue().getValidateCredResponseMsg();
            ValidateDeviceCredentialsResponse.ValidateDeviceCredentialsResponseBuilder result = ValidateDeviceCredentialsResponse.builder();
            if (msg.hasDeviceInfo()) {
                result.credentials(msg.getCredentialsBody());
                TransportDeviceInfo tdi = getTransportDeviceInfo(msg.getDeviceInfo());
                result.deviceInfo(tdi);
                ByteString profileBody = msg.getProfileBody();
                if (profileBody != null && !profileBody.isEmpty()) {
                    DeviceProfile profile = deviceProfileCache.getOrCreate(tdi.getDeviceProfileId(), profileBody);
                    if (transportType != DeviceTransportType.DEFAULT
                            && profile != null && profile.getTransportType() != DeviceTransportType.DEFAULT && profile.getTransportType() != transportType) {
                        log.debug("[{}] Device profile [{}] has different transport type: {}, expected: {}", tdi.getDeviceId(), tdi.getDeviceProfileId(), profile.getTransportType(), transportType);
                        throw new IllegalStateException("Device profile has different transport type: " + profile.getTransportType() + ". Expected: " + transportType);
                    }
                    result.deviceProfile(profile);
                }
            }
            return result.build();
        }, MoreExecutors.directExecutor());
        AsyncCallbackTemplate.withCallback(response, callback::onSuccess, callback::onError, transportCallbackExecutor);
    }

    @Override
    public void process(TransportProtos.GetOrCreateDeviceFromGatewayRequestMsg requestMsg, TransportServiceCallback<GetOrCreateDeviceFromGatewayResponse> callback) {
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setGetOrCreateDeviceRequestMsg(requestMsg).build());
        log.trace("Processing msg: {}", requestMsg);
        ListenableFuture<GetOrCreateDeviceFromGatewayResponse> response = Futures.transform(transportApiRequestTemplate.send(protoMsg), tmp -> {
            TransportProtos.GetOrCreateDeviceFromGatewayResponseMsg msg = tmp.getValue().getGetOrCreateDeviceResponseMsg();
            GetOrCreateDeviceFromGatewayResponse.GetOrCreateDeviceFromGatewayResponseBuilder result = GetOrCreateDeviceFromGatewayResponse.builder();
            if (msg.hasDeviceInfo()) {
                TransportDeviceInfo tdi = getTransportDeviceInfo(msg.getDeviceInfo());
                result.deviceInfo(tdi);
                ByteString profileBody = msg.getProfileBody();
                if (profileBody != null && !profileBody.isEmpty()) {
                    result.deviceProfile(deviceProfileCache.getOrCreate(tdi.getDeviceProfileId(), profileBody));
                }
            }
            return result.build();
        }, MoreExecutors.directExecutor());
        AsyncCallbackTemplate.withCallback(response, callback::onSuccess, callback::onError, transportCallbackExecutor);
    }

    private TransportDeviceInfo getTransportDeviceInfo(TransportProtos.DeviceInfoProto di) {
        TransportDeviceInfo tdi = new TransportDeviceInfo();
        tdi.setTenantId(new TenantId(new UUID(di.getTenantIdMSB(), di.getTenantIdLSB())));
        tdi.setDeviceId(new DeviceId(new UUID(di.getDeviceIdMSB(), di.getDeviceIdLSB())));
        tdi.setDeviceProfileId(new DeviceProfileId(new UUID(di.getDeviceProfileIdMSB(), di.getDeviceProfileIdLSB())));
        tdi.setAdditionalInfo(di.getAdditionalInfo());
        tdi.setDeviceName(di.getDeviceName());
        tdi.setDeviceType(di.getDeviceType());
        return tdi;
    }

    @Override
    public void process(ProvisionDeviceRequestMsg requestMsg, TransportServiceCallback<ProvisionDeviceResponseMsg> callback) {
        log.trace("Processing msg: {}", requestMsg);
        TbProtoQueueMsg<TransportApiRequestMsg> protoMsg = new TbProtoQueueMsg<>(UUID.randomUUID(), TransportApiRequestMsg.newBuilder().setProvisionDeviceRequestMsg(requestMsg).build());
        ListenableFuture<ProvisionDeviceResponseMsg> response = Futures.transform(transportApiRequestTemplate.send(protoMsg), tmp ->
                        tmp.getValue().getProvisionDeviceResponseMsg()
                , MoreExecutors.directExecutor());
        AsyncCallbackTemplate.withCallback(response, callback::onSuccess, callback::onError, transportCallbackExecutor);
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscriptionInfoProto msg, TransportServiceCallback<Void> callback) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", toSessionId(sessionInfo), msg);
        }
        sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                .setSubscriptionInfo(msg).build(), callback);
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SessionEventMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setSessionEvent(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostTelemetryMsg msg, TransportServiceCallback<Void> callback) {
        int dataPoints = 0;
        for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
            dataPoints += tsKv.getKvCount();
        }
        if (checkLimits(sessionInfo, msg, callback, dataPoints)) {
            reportActivityInternal(sessionInfo);
            TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            MsgPackCallback packCallback = new MsgPackCallback(msg.getTsKvListCount(), new ApiStatsProxyCallback<>(tenantId, dataPoints, callback));
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                TbMsgMetaData metaData = new TbMsgMetaData();
                metaData.putValue("deviceName", sessionInfo.getDeviceName());
                metaData.putValue("deviceType", sessionInfo.getDeviceType());
                metaData.putValue("ts", tsKv.getTs() + "");
                JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
                RuleChainId ruleChainId = resolveRuleChainId(sessionInfo);
                TbMsg tbMsg = TbMsg.newMsg(ServiceQueue.MAIN, SessionMsgType.POST_TELEMETRY_REQUEST.name(),
                        deviceId, metaData, gson.toJson(json), ruleChainId, null);
                sendToRuleEngine(tenantId, tbMsg, packCallback);
            }
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.PostAttributeMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback, msg.getKvCount())) {
            reportActivityInternal(sessionInfo);
            TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("deviceName", sessionInfo.getDeviceName());
            metaData.putValue("deviceType", sessionInfo.getDeviceType());
            metaData.putValue("notifyDevice", "false");
            RuleChainId ruleChainId = resolveRuleChainId(sessionInfo);
            TbMsg tbMsg = TbMsg.newMsg(ServiceQueue.MAIN, SessionMsgType.POST_ATTRIBUTES_REQUEST.name(),
                    deviceId, metaData, gson.toJson(json), ruleChainId, null);
            sendToRuleEngine(tenantId, tbMsg, new TransportTbQueueCallback(callback));
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.GetAttributeRequestMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setGetAttributes(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToAttributeUpdatesMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            SessionMetaData sessionMetaData = reportActivityInternal(sessionInfo);
            sessionMetaData.setSubscribedToAttributes(!msg.getUnsubscribe());
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setSubscribeToAttributes(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.SubscribeToRPCMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            SessionMetaData sessionMetaData = reportActivityInternal(sessionInfo);
            sessionMetaData.setSubscribedToRPC(!msg.getUnsubscribe());
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setSubscribeToRPC(msg).build(), callback);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToDeviceRpcResponseMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setToDeviceRPCCallResponse(msg).build(), callback);
        }
    }

    private void processTimeout(String requestId) {
        RpcRequestMetadata data = toServerRpcPendingMap.remove(requestId);
        if (data != null) {
            SessionMetaData md = sessions.get(data.getSessionId());
            if (md != null) {
                SessionMsgListener listener = md.getListener();
                transportCallbackExecutor.submit(() -> {
                    TransportProtos.ToServerRpcResponseMsg responseMsg =
                            TransportProtos.ToServerRpcResponseMsg.newBuilder()
                                    .setRequestId(data.getRequestId())
                                    .setError("timeout").build();
                    listener.onToServerRpcResponse(responseMsg);
                });
                if (md.getSessionType() == TransportProtos.SessionType.SYNC) {
                    deregisterSession(md.getSessionInfo());
                }
            } else {
                log.debug("[{}] Missing session.", data.getSessionId());
            }
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ToServerRpcRequestMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivityInternal(sessionInfo);
            UUID sessionId = toSessionId(sessionInfo);
            TenantId tenantId = getTenantId(sessionInfo);
            DeviceId deviceId = getDeviceId(sessionInfo);
            JsonObject json = new JsonObject();
            json.addProperty("method", msg.getMethodName());
            json.add("params", JsonUtils.parse(msg.getParams()));

            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("deviceName", sessionInfo.getDeviceName());
            metaData.putValue("deviceType", sessionInfo.getDeviceType());
            metaData.putValue("requestId", Integer.toString(msg.getRequestId()));
            metaData.putValue("serviceId", serviceInfoProvider.getServiceId());
            metaData.putValue("sessionId", sessionId.toString());
            RuleChainId ruleChainId = resolveRuleChainId(sessionInfo);
            TbMsg tbMsg = TbMsg.newMsg(ServiceQueue.MAIN, SessionMsgType.TO_SERVER_RPC_REQUEST.name(),
                    deviceId, metaData, gson.toJson(json), ruleChainId, null);
            sendToRuleEngine(tenantId, tbMsg, new TransportTbQueueCallback(callback));
            String requestId = sessionId + "-" + msg.getRequestId();
            toServerRpcPendingMap.put(requestId, new RpcRequestMetadata(sessionId, msg.getRequestId()));
            scheduler.schedule(() -> processTimeout(requestId), clientSideRpcTimeout, TimeUnit.MILLISECONDS);
        }
    }

    @Override
    public void process(TransportProtos.SessionInfoProto sessionInfo, TransportProtos.ClaimDeviceMsg msg, TransportServiceCallback<Void> callback) {
        if (checkLimits(sessionInfo, msg, callback)) {
            reportActivityInternal(sessionInfo);
            sendToDeviceActor(sessionInfo, TransportToDeviceActorMsg.newBuilder().setSessionInfo(sessionInfo)
                    .setClaimDevice(msg).build(), callback);
        }
    }

    @Override
    public void reportActivity(TransportProtos.SessionInfoProto sessionInfo) {
        reportActivityInternal(sessionInfo);
    }

    private SessionMetaData reportActivityInternal(TransportProtos.SessionInfoProto sessionInfo) {
        UUID sessionId = toSessionId(sessionInfo);
        SessionMetaData sessionMetaData = sessions.get(sessionId);
        if (sessionMetaData != null) {
            sessionMetaData.updateLastActivityTime();
        }
        return sessionMetaData;
    }

    private void checkInactivityAndReportActivity() {
        long expTime = System.currentTimeMillis() - sessionInactivityTimeout;
        sessions.forEach((uuid, sessionMD) -> {
            long lastActivityTime = sessionMD.getLastActivityTime();
            TransportProtos.SessionInfoProto sessionInfo = sessionMD.getSessionInfo();
            if (sessionInfo.getGwSessionIdMSB() > 0 &&
                    sessionInfo.getGwSessionIdLSB() > 0) {
                SessionMetaData gwMetaData = sessions.get(new UUID(sessionInfo.getGwSessionIdMSB(), sessionInfo.getGwSessionIdLSB()));
                if (gwMetaData != null) {
                    lastActivityTime = Math.max(gwMetaData.getLastActivityTime(), lastActivityTime);
                }
            }
            if (lastActivityTime < expTime) {
                if (log.isDebugEnabled()) {
                    log.debug("[{}] Session has expired due to last activity time: {}", toSessionId(sessionInfo), lastActivityTime);
                }
                process(sessionInfo, getSessionEventMsg(TransportProtos.SessionEvent.CLOSED), null);
                sessions.remove(uuid);
                sessionMD.getListener().onRemoteSessionCloseCommand(TransportProtos.SessionCloseNotificationProto.getDefaultInstance());
            } else {
                if (lastActivityTime > sessionMD.getLastReportedActivityTime()) {
                    final long lastActivityTimeFinal = lastActivityTime;
                    process(sessionInfo, TransportProtos.SubscriptionInfoProto.newBuilder()
                            .setAttributeSubscription(sessionMD.isSubscribedToAttributes())
                            .setRpcSubscription(sessionMD.isSubscribedToRPC())
                            .setLastActivityTime(lastActivityTime).build(), new TransportServiceCallback<Void>() {
                        @Override
                        public void onSuccess(Void msg) {
                            sessionMD.setLastReportedActivityTime(lastActivityTimeFinal);
                        }

                        @Override
                        public void onError(Throwable e) {
                            log.warn("[{}] Failed to report last activity time", uuid, e);
                        }
                    });
                }
            }
        });
    }

    @Override
    public void registerSyncSession(TransportProtos.SessionInfoProto sessionInfo, SessionMsgListener listener, long timeout) {
        SessionMetaData currentSession = new SessionMetaData(sessionInfo, TransportProtos.SessionType.SYNC, listener);
        sessions.putIfAbsent(toSessionId(sessionInfo), currentSession);

        ScheduledFuture executorFuture = scheduler.schedule(() -> {
            listener.onRemoteSessionCloseCommand(TransportProtos.SessionCloseNotificationProto.getDefaultInstance());
            deregisterSession(sessionInfo);
        }, timeout, TimeUnit.MILLISECONDS);

        currentSession.setScheduledFuture(executorFuture);
    }

    @Override
    public void deregisterSession(TransportProtos.SessionInfoProto sessionInfo) {
        SessionMetaData currentSession = sessions.get(toSessionId(sessionInfo));
        if (currentSession != null && currentSession.hasScheduledFuture()) {
            log.debug("Stopping scheduler to avoid resending response if request has been ack.");
            currentSession.getScheduledFuture().cancel(false);
        }
        sessions.remove(toSessionId(sessionInfo));
    }

    private boolean checkLimits(TransportProtos.SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<Void> callback) {
        return checkLimits(sessionInfo, msg, callback, 0);
    }

    private boolean checkLimits(TransportProtos.SessionInfoProto sessionInfo, Object msg, TransportServiceCallback<Void> callback, int dataPoints) {
        if (log.isTraceEnabled()) {
            log.trace("[{}] Processing msg: {}", toSessionId(sessionInfo), msg);
        }
        TenantId tenantId = new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
        DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));

        EntityType rateLimitedEntityType = rateLimitService.checkLimits(tenantId, deviceId, dataPoints);
        if (rateLimitedEntityType == null) {
            return true;
        } else {
            if (callback != null) {
                callback.onError(new TbRateLimitsException(rateLimitedEntityType));
            }
            return false;
        }
    }

    protected void processToTransportMsg(TransportProtos.ToTransportMsg toSessionMsg) {
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
                    String requestId = sessionId + "-" + toSessionMsg.getToServerResponse().getRequestId();
                    toServerRpcPendingMap.remove(requestId);
                    listener.onToServerRpcResponse(toSessionMsg.getToServerResponse());
                }
            });
            if (md.getSessionType() == TransportProtos.SessionType.SYNC) {
                deregisterSession(md.getSessionInfo());
            }
        } else {
            if (toSessionMsg.hasEntityUpdateMsg()) {
                TransportProtos.EntityUpdateMsg msg = toSessionMsg.getEntityUpdateMsg();
                EntityType entityType = EntityType.valueOf(msg.getEntityType());
                if (EntityType.DEVICE_PROFILE.equals(entityType)) {
                    DeviceProfile deviceProfile = deviceProfileCache.put(msg.getData());
                    if (deviceProfile != null) {
                        onProfileUpdate(deviceProfile);
                    }
                } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
                    rateLimitService.update(tenantProfileCache.put(msg.getData()));
                } else if (EntityType.TENANT.equals(entityType)) {
                    Optional<Tenant> profileOpt = dataDecodingEncodingService.decode(msg.getData().toByteArray());
                    if (profileOpt.isPresent()) {
                        Tenant tenant = profileOpt.get();
                        boolean updated = tenantProfileCache.put(tenant.getId(), tenant.getTenantProfileId());
                        if (updated) {
                            rateLimitService.update(tenant.getId());
                        }
                    }
                } else if (EntityType.API_USAGE_STATE.equals(entityType)) {
                    Optional<ApiUsageState> stateOpt = dataDecodingEncodingService.decode(msg.getData().toByteArray());
                    if (stateOpt.isPresent()) {
                        ApiUsageState apiUsageState = stateOpt.get();
                        rateLimitService.update(apiUsageState.getTenantId(), apiUsageState.isTransportEnabled());
                    }
                }
            } else if (toSessionMsg.hasEntityDeleteMsg()) {
                TransportProtos.EntityDeleteMsg msg = toSessionMsg.getEntityDeleteMsg();
                EntityType entityType = EntityType.valueOf(msg.getEntityType());
                UUID entityUuid = new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB());
                if (EntityType.DEVICE_PROFILE.equals(entityType)) {
                    deviceProfileCache.evict(new DeviceProfileId(new UUID(msg.getEntityIdMSB(), msg.getEntityIdLSB())));
                } else if (EntityType.TENANT_PROFILE.equals(entityType)) {
                    tenantProfileCache.remove(new TenantProfileId(entityUuid));
                } else if (EntityType.TENANT.equals(entityType)) {
                    rateLimitService.remove(new TenantId(entityUuid));
                } else if (EntityType.DEVICE.equals(entityType)) {
                    onDeviceDelete(msg);
                    rateLimitService.remove(new DeviceId(entityUuid));
                }
            } else {
                //TODO: should we notify the device actor about missed session?
                log.debug("[{}] Missing session.", sessionId);
            }
        }
    }

    private void onProfileUpdate(DeviceProfile deviceProfile) {
        long deviceProfileIdMSB = deviceProfile.getId().getId().getMostSignificantBits();
        long deviceProfileIdLSB = deviceProfile.getId().getId().getLeastSignificantBits();
        sessions.forEach((id, md) -> {
            if (md.getSessionInfo().getDeviceProfileIdMSB() == deviceProfileIdMSB
                    && md.getSessionInfo().getDeviceProfileIdLSB() == deviceProfileIdLSB) {
                transportCallbackExecutor.submit(() -> md.getListener().onProfileUpdate(deviceProfile));
            }
        });
    }

    @Override
    public void onDeviceDelete(EntityDeleteMsg msg) {
        long deviceIdMSB = msg.getEntityIdMSB();
        long deviceIdLSB = msg.getEntityIdLSB();
        sessions.forEach((id, md) -> {
            if (md.getSessionInfo().getDeviceIdMSB() == deviceIdMSB
                    && md.getSessionInfo().getDeviceIdLSB() == deviceIdLSB) {
                transportCallbackExecutor.submit(() -> md.getListener().onDeviceDeleted(msg));
            }
        });
    }

    protected UUID toSessionId(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getSessionIdMSB(), sessionInfo.getSessionIdLSB());
    }

    protected UUID getRoutingKey(TransportProtos.SessionInfoProto sessionInfo) {
        return new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB());
    }

    protected TenantId getTenantId(TransportProtos.SessionInfoProto sessionInfo) {
        return new TenantId(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
    }

    protected DeviceId getDeviceId(TransportProtos.SessionInfoProto sessionInfo) {
        return new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
    }

    public static TransportProtos.SessionEventMsg getSessionEventMsg(TransportProtos.SessionEvent event) {
        return TransportProtos.SessionEventMsg.newBuilder()
                .setSessionType(TransportProtos.SessionType.ASYNC)
                .setEvent(event).build();
    }

    protected void sendToDeviceActor(TransportProtos.SessionInfoProto sessionInfo, TransportToDeviceActorMsg toDeviceActorMsg, TransportServiceCallback<Void> callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_CORE, getTenantId(sessionInfo), getDeviceId(sessionInfo));
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", getTenantId(sessionInfo), getDeviceId(sessionInfo), tpi.getFullTopicName(), toDeviceActorMsg);
        }
        TransportTbQueueCallback transportTbQueueCallback = callback != null ?
                new TransportTbQueueCallback(callback) : null;
        tbCoreProducerStats.incrementTotal();
        StatsCallback wrappedCallback = new StatsCallback(transportTbQueueCallback, tbCoreProducerStats);
        tbCoreMsgProducer.send(tpi,
                new TbProtoQueueMsg<>(getRoutingKey(sessionInfo),
                        ToCoreMsg.newBuilder().setToDeviceActorMsg(toDeviceActorMsg).build()),
                wrappedCallback);
    }

    protected void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        TopicPartitionInfo tpi = partitionService.resolve(ServiceType.TB_RULE_ENGINE, tenantId, tbMsg.getOriginator());
        if (log.isTraceEnabled()) {
            log.trace("[{}][{}] Pushing to topic {} message {}", tenantId, tbMsg.getOriginator(), tpi.getFullTopicName(), tbMsg);
        }
        ToRuleEngineMsg msg = ToRuleEngineMsg.newBuilder().setTbMsg(TbMsg.toByteString(tbMsg))
                .setTenantIdMSB(tenantId.getId().getMostSignificantBits())
                .setTenantIdLSB(tenantId.getId().getLeastSignificantBits()).build();
        ruleEngineProducerStats.incrementTotal();
        StatsCallback wrappedCallback = new StatsCallback(callback, ruleEngineProducerStats);
        ruleEngineMsgProducer.send(tpi, new TbProtoQueueMsg<>(tbMsg.getId(), msg), wrappedCallback);
    }

    private RuleChainId resolveRuleChainId(TransportProtos.SessionInfoProto sessionInfo) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));
        DeviceProfile deviceProfile = deviceProfileCache.get(deviceProfileId);
        RuleChainId ruleChainId;
        if (deviceProfile == null) {
            log.warn("[{}] Device profile is null!", deviceProfileId);
            ruleChainId = null;
        } else {
            ruleChainId = deviceProfile.getDefaultRuleChainId();
        }
        return ruleChainId;
    }

    private class TransportTbQueueCallback implements TbQueueCallback {
        private final TransportServiceCallback<Void> callback;

        private TransportTbQueueCallback(TransportServiceCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            DefaultTransportService.this.transportCallbackExecutor.submit(() -> callback.onSuccess(null));
        }

        @Override
        public void onFailure(Throwable t) {
            DefaultTransportService.this.transportCallbackExecutor.submit(() -> callback.onError(t));
        }
    }

    private static class StatsCallback implements TbQueueCallback {
        private final TbQueueCallback callback;
        private final MessagesStats stats;

        private StatsCallback(TbQueueCallback callback, MessagesStats stats) {
            this.callback = callback;
            this.stats = stats;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            stats.incrementSuccessful();
            if (callback != null)
                callback.onSuccess(metadata);
        }

        @Override
        public void onFailure(Throwable t) {
            stats.incrementFailed();
            if (callback != null)
                callback.onFailure(t);
        }
    }

    private class MsgPackCallback implements TbQueueCallback {
        private final AtomicInteger msgCount;
        private final TransportServiceCallback<Void> callback;

        public MsgPackCallback(Integer msgCount, TransportServiceCallback<Void> callback) {
            this.msgCount = new AtomicInteger(msgCount);
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (msgCount.decrementAndGet() <= 0) {
                DefaultTransportService.this.transportCallbackExecutor.submit(() -> callback.onSuccess(null));
            }
        }

        @Override
        public void onFailure(Throwable t) {
            callback.onError(t);
        }
    }

    private class ApiStatsProxyCallback<T> implements TransportServiceCallback<T> {
        private final TenantId tenantId;
        private final int dataPoints;
        private final TransportServiceCallback<T> callback;

        public ApiStatsProxyCallback(TenantId tenantId, int dataPoints, TransportServiceCallback<T> callback) {
            this.tenantId = tenantId;
            this.dataPoints = dataPoints;
            this.callback = callback;
        }

        @Override
        public void onSuccess(T msg) {
            try {
                apiUsageStatsClient.report(tenantId, ApiUsageRecordKey.TRANSPORT_MSG_COUNT, 1);
                apiUsageStatsClient.report(tenantId, ApiUsageRecordKey.TRANSPORT_DP_COUNT, dataPoints);
            } finally {
                callback.onSuccess(msg);
            }
        }

        @Override
        public void onError(Throwable e) {
            callback.onError(e);
        }
    }
}
