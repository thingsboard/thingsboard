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
package org.thingsboard.server.actors;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.DeviceStateManager;
import org.thingsboard.rule.engine.api.JobManager;
import org.thingsboard.rule.engine.api.MailService;
import org.thingsboard.rule.engine.api.MqttClientSettings;
import org.thingsboard.rule.engine.api.NotificationCenter;
import org.thingsboard.rule.engine.api.RuleEngineAiChatModelService;
import org.thingsboard.rule.engine.api.SmsService;
import org.thingsboard.rule.engine.api.notification.SlackService;
import org.thingsboard.rule.engine.api.sms.SmsSenderFactory;
import org.thingsboard.script.api.js.JsInvokeService;
import org.thingsboard.script.api.tbel.TbelInvokeService;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.actors.tenant.DebugTbRateLimits;
import org.thingsboard.server.cache.limits.RateLimitService;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.event.CalculatedFieldDebugEvent;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.RuleChainDebugEvent;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.limit.LimitedApi;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbActorMsg;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.notification.NotificationRuleProcessor;
import org.thingsboard.server.common.msg.queue.ServiceType;
import org.thingsboard.server.common.msg.queue.TopicPartitionInfo;
import org.thingsboard.server.common.msg.tools.TbRateLimits;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.dao.ai.AiModelService;
import org.thingsboard.server.dao.alarm.AlarmCommentService;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.cassandra.CassandraCluster;
import org.thingsboard.server.dao.cf.CalculatedFieldService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.ClaimDevicesService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.domain.DomainService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.job.JobService;
import org.thingsboard.server.dao.mobile.MobileAppBundleService;
import org.thingsboard.server.dao.mobile.MobileAppService;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateReadExecutor;
import org.thingsboard.server.dao.nosql.CassandraBufferedRateWriteExecutor;
import org.thingsboard.server.dao.notification.NotificationRequestService;
import org.thingsboard.server.dao.notification.NotificationRuleService;
import org.thingsboard.server.dao.notification.NotificationTargetService;
import org.thingsboard.server.dao.notification.NotificationTemplateService;
import org.thingsboard.server.dao.oauth2.OAuth2ClientService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.queue.QueueStatsService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.resource.TbResourceDataCache;
import org.thingsboard.server.dao.resource.ResourceService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.rule.RuleNodeStateService;
import org.thingsboard.server.dao.tenant.TbTenantProfileCache;
import org.thingsboard.server.dao.tenant.TenantProfileService;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.discovery.DiscoveryService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.discovery.TbServiceInfoProvider;
import org.thingsboard.server.queue.settings.TbQueueCalculatedFieldSettings;
import org.thingsboard.server.service.apiusage.TbApiUsageStateService;
import org.thingsboard.server.service.cf.CalculatedFieldProcessingService;
import org.thingsboard.server.service.cf.CalculatedFieldQueueService;
import org.thingsboard.server.service.cf.CalculatedFieldStateService;
import org.thingsboard.server.service.cf.ctx.state.ArgumentEntry;
import org.thingsboard.server.service.component.ComponentDiscoveryService;
import org.thingsboard.server.service.edge.rpc.EdgeRpcService;
import org.thingsboard.server.service.entitiy.entityview.TbEntityViewService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.executors.ExternalCallExecutorService;
import org.thingsboard.server.service.executors.NotificationExecutorService;
import org.thingsboard.server.service.executors.PubSubRuleNodeExecutorProvider;
import org.thingsboard.server.service.executors.SharedEventLoopGroupService;
import org.thingsboard.server.service.mail.MailExecutorService;
import org.thingsboard.server.service.profile.TbAssetProfileCache;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.rpc.TbCoreDeviceRpcService;
import org.thingsboard.server.service.rpc.TbRpcService;
import org.thingsboard.server.service.rpc.TbRuleEngineDeviceRpcService;
import org.thingsboard.server.service.session.DeviceSessionCacheService;
import org.thingsboard.server.service.sms.SmsExecutorService;
import org.thingsboard.server.service.state.DeviceStateService;
import org.thingsboard.server.service.telemetry.AlarmSubscriptionService;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;
import org.thingsboard.server.service.transport.TbCoreToTransportService;
import org.thingsboard.server.utils.DebugModeRateLimitsConfig;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
public class ActorSystemContext {

    private static final FutureCallback<Void> RULE_CHAIN_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Rule Chain", th);
        }
    };
    private static final FutureCallback<Void> RULE_NODE_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Node", th);
        }
    };

    private static final FutureCallback<Void> CALCULATED_FIELD_DEBUG_EVENT_ERROR_CALLBACK = new FutureCallback<>() {
        @Override
        public void onSuccess(@Nullable Void event) {

        }

        @Override
        public void onFailure(Throwable th) {
            log.error("Could not save debug Event for Calculated Field", th);
        }
    };

    private final ConcurrentMap<TenantId, DebugTbRateLimits> debugPerTenantLimits = new ConcurrentHashMap<>();

    public ConcurrentMap<TenantId, DebugTbRateLimits> getDebugPerTenantLimits() {
        return debugPerTenantLimits;
    }

    @Autowired
    @Getter
    private TbApiUsageStateService apiUsageStateService;

    @Autowired
    @Getter
    private TbApiUsageReportClient apiUsageClient;

    @Autowired
    @Getter
    @Setter
    private TbServiceInfoProvider serviceInfoProvider;

    @Getter
    @Setter
    private ActorService actorService;

    @Autowired
    @Getter
    @Setter
    private ComponentDiscoveryService componentService;

    @Autowired
    @Getter
    private DiscoveryService discoveryService;

    @Autowired
    @Getter
    private DeviceService deviceService;

    @Autowired
    @Getter
    private DeviceProfileService deviceProfileService;

    @Autowired
    @Getter
    private AssetProfileService assetProfileService;

    @Autowired
    @Getter
    private DeviceCredentialsService deviceCredentialsService;

    @Autowired(required = false)
    @Getter
    private DeviceStateManager deviceStateManager;

    @Autowired
    @Getter
    private TbTenantProfileCache tenantProfileCache;

    @Autowired
    @Getter
    private TbDeviceProfileCache deviceProfileCache;

    @Autowired
    @Getter
    private TbAssetProfileCache assetProfileCache;

    @Autowired
    @Getter
    private AssetService assetService;

    @Autowired
    @Getter
    private DashboardService dashboardService;

    @Autowired
    @Getter
    private TenantService tenantService;

    @Autowired
    @Getter
    private TenantProfileService tenantProfileService;

    @Autowired
    @Getter
    private CustomerService customerService;

    @Autowired
    @Getter
    private UserService userService;

    @Autowired
    @Getter
    private RuleChainService ruleChainService;

    @Autowired
    @Getter
    private RuleNodeStateService ruleNodeStateService;

    @Autowired
    @Getter
    private PartitionService partitionService;

    @Autowired
    @Getter
    private TbClusterService clusterService;

    @Autowired
    @Getter
    private TimeseriesService tsService;

    @Autowired
    @Getter
    private AttributesService attributesService;

    @Autowired
    @Getter
    private EventService eventService;

    @Autowired
    @Getter
    private RelationService relationService;

    @Autowired
    @Getter
    private AuditLogService auditLogService;

    @Autowired
    @Getter
    private RuleEngineAiChatModelService aiChatModelService;

    @Autowired
    @Getter
    private AiModelService aiModelService;

    @Autowired
    @Getter
    private EntityViewService entityViewService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private TbEntityViewService tbEntityViewService;

    @Autowired
    @Getter
    private TelemetrySubscriptionService tsSubService;

    @Autowired
    @Getter
    private AlarmSubscriptionService alarmService;

    @Autowired
    @Getter
    private AlarmCommentService alarmCommentService;

    @Autowired
    @Getter
    private JsInvokeService jsInvokeService;

    @Autowired(required = false)
    @Getter
    private TbelInvokeService tbelInvokeService;

    @Autowired
    @Getter
    private MailExecutorService mailExecutor;

    @Autowired
    @Getter
    private SmsExecutorService smsExecutor;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    @Getter
    private ExternalCallExecutorService externalCallExecutorService;

    @Autowired
    @Getter
    private NotificationExecutorService notificationExecutor;

    @Lazy
    @Autowired
    @Getter
    private PubSubRuleNodeExecutorProvider pubSubRuleNodeExecutorProvider;

    @Autowired
    @Getter
    private SharedEventLoopGroupService sharedEventLoopGroupService;

    @Autowired
    @Getter
    private MailService mailService;

    @Autowired
    @Getter
    private SmsService smsService;

    @Autowired
    @Getter
    private SmsSenderFactory smsSenderFactory;

    @Autowired
    @Getter
    private NotificationCenter notificationCenter;

    @Autowired
    @Getter
    private NotificationRuleProcessor notificationRuleProcessor;

    @Autowired
    @Getter
    private NotificationTargetService notificationTargetService;

    @Autowired
    @Getter
    private NotificationTemplateService notificationTemplateService;

    @Autowired
    @Getter
    private NotificationRequestService notificationRequestService;

    @Autowired
    @Getter
    private NotificationRuleService notificationRuleService;

    @Autowired
    @Getter
    private OAuth2ClientService oAuth2ClientService;

    @Autowired
    @Getter
    private DomainService domainService;

    @Autowired
    @Getter
    private MobileAppService mobileAppService;

    @Autowired
    @Getter
    private MobileAppBundleService mobileAppBundleService;

    @Autowired
    @Getter
    private SlackService slackService;

    @Autowired
    @Getter
    private CalculatedFieldService calculatedFieldService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private ClaimDevicesService claimDevicesService;

    //TODO: separate context for TbCore and TbRuleEngine
    @Autowired(required = false)
    @Getter
    private DeviceStateService deviceStateService;

    @Autowired(required = false)
    @Getter
    private DeviceSessionCacheService deviceSessionCacheService;

    @Autowired(required = false)
    @Getter
    private TbCoreToTransportService tbCoreToTransportService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private ApiLimitService apiLimitService;

    @Autowired(required = false)
    @Getter
    private RateLimitService rateLimitService;

    @Autowired(required = false)
    @Getter
    private DebugModeRateLimitsConfig debugModeRateLimitsConfig;

    @Lazy
    @Autowired(required = false)
    @Getter
    private TbQueueCalculatedFieldSettings calculatedFieldSettings;

    /**
     * The following Service will be null if we operate in tb-core mode
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRuleEngineDeviceRpcService tbRuleEngineDeviceRpcService;

    /**
     * The following Service will be null if we operate in tb-rule-engine mode
     */
    @Lazy
    @Autowired(required = false)
    @Getter
    private TbCoreDeviceRpcService tbCoreDeviceRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeService edgeService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeEventService edgeEventService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EdgeRpcService edgeRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private ResourceService resourceService;

    @Autowired
    @Getter
    private TbResourceDataCache resourceDataCache;

    @Lazy
    @Autowired(required = false)
    @Getter
    private OtaPackageService otaPackageService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private TbRpcService tbRpcService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private QueueService queueService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private QueueStatsService queueStatsService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private WidgetsBundleService widgetsBundleService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private WidgetTypeService widgetTypeService;

    @Lazy
    @Autowired(required = false)
    @Getter
    private EntityService entityService;

    @Autowired(required = false)
    @Getter
    private CalculatedFieldProcessingService calculatedFieldProcessingService;

    @Autowired(required = false)
    @Getter
    private CalculatedFieldStateService calculatedFieldStateService;

    @Autowired(required = false)
    @Getter
    private CalculatedFieldQueueService calculatedFieldQueueService;

    @Autowired
    @Getter
    private JobService jobService;

    @Autowired
    @Getter
    private JobManager jobManager;

    @Value("${actors.session.max_concurrent_sessions_per_device:1}")
    @Getter
    private int maxConcurrentSessionsPerDevice;

    @Value("${actors.session.sync.timeout:10000}")
    @Getter
    private long syncSessionTimeout;

    @Value("${actors.rule.chain.error_persist_frequency:3000}")
    @Getter
    private long ruleChainErrorPersistFrequency;

    @Value("${actors.rule.node.error_persist_frequency:3000}")
    @Getter
    private long ruleNodeErrorPersistFrequency;

    @Value("${actors.statistics.enabled:true}")
    @Getter
    private boolean statisticsEnabled;

    @Value("${actors.statistics.persist_frequency:3600000}")
    @Getter
    private long statisticsPersistFrequency;

    @Value("${edges.enabled:true}")
    @Getter
    private boolean edgesEnabled;

    @Value("${cache.type:caffeine}")
    @Getter
    private String cacheType;

    @Getter
    private boolean localCacheType;

    @PostConstruct
    public void init() {
        this.localCacheType = "caffeine".equals(cacheType);
    }

    @Value("${actors.tenant.create_components_on_init:true}")
    @Getter
    private boolean tenantComponentsInitEnabled;

    @Value("${actors.rule.allow_system_mail_service:true}")
    @Getter
    private boolean allowSystemMailService;

    @Value("${actors.rule.allow_system_sms_service:true}")
    @Getter
    private boolean allowSystemSmsService;

    @Value("${transport.sessions.inactivity_timeout:300000}")
    @Getter
    private long sessionInactivityTimeout;

    @Value("${transport.sessions.report_timeout:3000}")
    @Getter
    private long sessionReportTimeout;

    @Value("${actors.rpc.submit_strategy:BURST}")
    @Getter
    private String rpcSubmitStrategy;

    @Value("${actors.rpc.close_session_on_rpc_delivery_timeout:false}")
    @Getter
    private boolean closeTransportSessionOnRpcDeliveryTimeout;

    @Value("${actors.rpc.response_timeout_ms:30000}")
    @Getter
    private long rpcResponseTimeout;

    @Value("${actors.rpc.max_retries:5}")
    @Getter
    private int maxRpcRetries;

    @Value("${actors.rule.external.force_ack:false}")
    @Getter
    private boolean externalNodeForceAck;

    @Value("${state.rule.node.deviceState.rateLimit:1:1,30:60,60:3600}")
    @Getter
    private String deviceStateNodeRateLimitConfig;

    @Value("${actors.calculated_fields.calculation_timeout:5}")
    @Getter
    private long cfCalculationResultTimeout;

    @Autowired
    @Getter
    private MqttClientSettings mqttClientSettings;

    @Getter
    @Setter
    private TbActorSystem actorSystem;

    @Setter
    private TbActorRef appActor;

    @Getter
    @Setter
    private TbActorRef statsActor;

    @Autowired(required = false)
    @Getter
    private CassandraCluster cassandraCluster;

    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateReadExecutor cassandraBufferedRateReadExecutor;

    @Autowired(required = false)
    @Getter
    private CassandraBufferedRateWriteExecutor cassandraBufferedRateWriteExecutor;

    @Autowired(required = false)
    @Getter
    private RedisTemplate<String, Object> redisTemplate;

    public ScheduledExecutorService getScheduler() {
        return actorSystem.getScheduler();
    }

    public void persistError(TenantId tenantId, EntityId entityId, String method, Exception e) {
        eventService.saveAsync(ErrorEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .method(method)
                .error(toString(e)).build());
    }

    public void persistLifecycleEvent(TenantId tenantId, EntityId entityId, ComponentLifecycleEvent lcEvent, Exception e) {
        LifecycleEvent.LifecycleEventBuilder event = LifecycleEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .lcEventType(lcEvent.name());

        if (e != null) {
            event.success(false).error(toString(e));
        } else {
            event.success(true);
        }

        eventService.saveAsync(event.build());
    }

    private String toString(Throwable e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    public TopicPartitionInfo resolve(ServiceType serviceType, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, tenantId, entityId);
    }

    public TopicPartitionInfo resolve(ServiceType serviceType, String queueName, TenantId tenantId, EntityId entityId) {
        return partitionService.resolve(serviceType, queueName, tenantId, entityId);
    }

    public TopicPartitionInfo resolve(TenantId tenantId, EntityId entityId, TbMsg msg) {
        return partitionService.resolve(ServiceType.TB_RULE_ENGINE, msg.getQueueName(), tenantId, entityId, msg.getPartition());
    }

    public String getServiceId() {
        return serviceInfoProvider.getServiceId();
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, null, null);
    }

    public void persistDebugInput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "IN", tbMsg, relationType, error, null);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error, String failureMessage) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error, failureMessage);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType, Throwable error) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, error, null);
    }

    public void persistDebugOutput(TenantId tenantId, EntityId entityId, TbMsg tbMsg, String relationType) {
        persistDebugAsync(tenantId, entityId, "OUT", tbMsg, relationType, null, null);
    }

    private void persistDebugAsync(TenantId tenantId, EntityId entityId, String type, TbMsg tbMsg, String relationType, Throwable error, String failureMessage) {
        if (checkLimits(tenantId, tbMsg, error)) {
            try {
                RuleNodeDebugEvent.RuleNodeDebugEventBuilder event = RuleNodeDebugEvent.builder()
                        .tenantId(tenantId)
                        .entityId(entityId.getId())
                        .serviceId(getServiceId())
                        .eventType(type)
                        .eventEntity(tbMsg.getOriginator())
                        .msgId(tbMsg.getId())
                        .msgType(tbMsg.getType())
                        .dataType(tbMsg.getDataType().name())
                        .relationType(relationType)
                        .data(tbMsg.getData())
                        .metadata(JacksonUtil.toString(tbMsg.getMetaData().getData()));

                if (error != null) {
                    event.error(toString(error));
                } else if (failureMessage != null) {
                    event.error(failureMessage);
                }

                ListenableFuture<Void> future = eventService.saveAsync(event.build());
                Futures.addCallback(future, RULE_NODE_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
            } catch (IllegalArgumentException ex) {
                log.warn("Failed to persist rule node debug message", ex);
            }
        }
    }

    private boolean checkLimits(TenantId tenantId, TbMsg tbMsg, Throwable error) {
        if (debugModeRateLimitsConfig.isRuleChainDebugPerTenantLimitsEnabled()) {
            DebugTbRateLimits debugTbRateLimits = debugPerTenantLimits.computeIfAbsent(tenantId, id ->
                    new DebugTbRateLimits(new TbRateLimits(debugModeRateLimitsConfig.getRuleChainDebugPerTenantLimitsConfiguration()), false));

            if (!debugTbRateLimits.getTbRateLimits().tryConsume()) {
                if (!debugTbRateLimits.isRuleChainEventSaved()) {
                    persistRuleChainDebugModeEvent(tenantId, tbMsg.getRuleChainId(), error);
                    debugTbRateLimits.setRuleChainEventSaved(true);
                }
                if (log.isTraceEnabled()) {
                    log.trace("[{}] Tenant level debug mode rate limit detected: {}", tenantId, tbMsg);
                }
                return false;
            }
        }
        return true;
    }

    private void persistRuleChainDebugModeEvent(TenantId tenantId, EntityId entityId, Throwable error) {
        RuleChainDebugEvent.RuleChainDebugEventBuilder event = RuleChainDebugEvent.builder()
                .tenantId(tenantId)
                .entityId(entityId.getId())
                .serviceId(getServiceId())
                .message("Reached debug mode rate limit!");
        if (error != null) {
            event.error(toString(error));
        }

        ListenableFuture<Void> future = eventService.saveAsync(event.build());
        Futures.addCallback(future, RULE_CHAIN_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
    }

    public void persistCalculatedFieldDebugEvent(TenantId tenantId, CalculatedFieldId calculatedFieldId, EntityId entityId, Map<String, ArgumentEntry> arguments, UUID tbMsgId, TbMsgType tbMsgType, String result, String errorMessage) {
        if (checkLimits(tenantId)) {
            try {
                CalculatedFieldDebugEvent.CalculatedFieldDebugEventBuilder eventBuilder = CalculatedFieldDebugEvent.builder()
                        .tenantId(tenantId)
                        .entityId(calculatedFieldId.getId())
                        .serviceId(getServiceId())
                        .calculatedFieldId(calculatedFieldId)
                        .eventEntity(entityId);
                if (tbMsgId != null) {
                    eventBuilder.msgId(tbMsgId);
                }
                if (tbMsgType != null) {
                    eventBuilder.msgType(tbMsgType.name());
                }
                if (arguments != null) {
                    eventBuilder.arguments(JacksonUtil.toString(
                            arguments.entrySet().stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toTbelCfArg()))
                    ));
                }
                if (result != null) {
                    eventBuilder.result(result);
                }
                if (errorMessage != null) {
                    eventBuilder.error(errorMessage);
                }

                ListenableFuture<Void> future = eventService.saveAsync(eventBuilder.build());
                Futures.addCallback(future, CALCULATED_FIELD_DEBUG_EVENT_ERROR_CALLBACK, MoreExecutors.directExecutor());
            } catch (IllegalArgumentException ex) {
                log.warn("Failed to persist calculated field debug message", ex);
            }
        }
    }

    private boolean checkLimits(TenantId tenantId) {
        if (debugModeRateLimitsConfig.isCalculatedFieldDebugPerTenantLimitsEnabled() &&
                !rateLimitService.checkRateLimit(LimitedApi.CALCULATED_FIELD_DEBUG_EVENTS, (Object) tenantId, debugModeRateLimitsConfig.getCalculatedFieldDebugPerTenantLimitsConfiguration())) {
            log.trace("[{}] Calculated field debug event limits exceeded!", tenantId);
            return false;
        }
        return true;
    }

    public static Exception toException(Throwable error) {
        return Exception.class.isInstance(error) ? (Exception) error : new Exception(error);
    }

    public void tell(TbActorMsg tbActorMsg) {
        appActor.tell(tbActorMsg);
    }

    public void tellWithHighPriority(TbActorMsg tbActorMsg) {
        appActor.tellWithHighPriority(tbActorMsg);
    }

    public ScheduledFuture<?> schedulePeriodicMsgWithDelay(TbActorRef ctx, TbActorMsg msg, long delayInMs, long periodInMs) {
        log.debug("Scheduling periodic msg {} every {} ms with delay {} ms", msg, periodInMs, delayInMs);
        return getScheduler().scheduleWithFixedDelay(() -> ctx.tell(msg), delayInMs, periodInMs, TimeUnit.MILLISECONDS);
    }

    public void scheduleMsgWithDelay(TbActorRef ctx, TbActorMsg msg, long delayInMs) {
        log.debug("Scheduling msg {} with delay {} ms", msg, delayInMs);
        if (delayInMs > 0) {
            getScheduler().schedule(() -> ctx.tell(msg), delayInMs, TimeUnit.MILLISECONDS);
        } else {
            ctx.tell(msg);
        }
    }

}
