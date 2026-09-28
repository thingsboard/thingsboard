// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.thingsboard.common.util.DonAsynchron;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.integration.api.IntegrationCallback;
import org.thingsboard.rule.engine.api.TimeseriesSaveRequest;
import org.thingsboard.server.common.data.ApiUsageRecordKey;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.JavaSerDesUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.group.EntityGroup;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.ConverterId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.IntegrationId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.integration.AbstractIntegration;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.kv.BaseAttributeKvEntry;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.TsKvEntry;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.objects.TelemetryEntityView;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.common.stats.TbApiUsageReportClient;
import org.thingsboard.server.common.transport.util.JsonUtils;
import org.thingsboard.server.common.util.KvProtoUtil;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.group.EntityGroupService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.exception.ThingsboardRuntimeException;
import org.thingsboard.server.gen.integration.AssetUplinkDataProto;
import org.thingsboard.server.gen.integration.DeviceUplinkDataProto;
import org.thingsboard.server.gen.integration.EntityViewDataProto;
import org.thingsboard.server.gen.integration.TbIntegrationEventProto;
import org.thingsboard.server.gen.integration.TbIntegrationTsDataProto;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceActivityProto;
import org.thingsboard.server.gen.transport.TransportProtos.PostAttributeMsg;
import org.thingsboard.server.gen.transport.TransportProtos.PostTelemetryMsg;
import org.thingsboard.server.gen.transport.TransportProtos.SessionInfoProto;
import org.thingsboard.server.queue.TbQueueCallback;
import org.thingsboard.server.queue.TbQueueMsgMetadata;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.common.TbRuleEngineProducerService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.profile.DefaultTbAssetProfileCache;
import org.thingsboard.server.service.profile.DefaultTbDeviceProfileCache;
import org.thingsboard.server.service.telemetry.TelemetrySubscriptionService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@TbCoreComponent
public class DefaultPlatformIntegrationService extends IntegrationActivityManager implements PlatformIntegrationService {

    private final ConcurrentMap<TenantId, RefCountedLock> entityCreationLocks = new ConcurrentHashMap<>();

    @Autowired
    private EventService eventService;

    @Autowired
    @Lazy
    private TbQueueProducerProvider producerProvider;

    @Autowired
    private TbRuleEngineProducerService ruleEngineProducerService;

    @Autowired
    private TelemetrySubscriptionService telemetrySubscriptionService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private EntityGroupService entityGroupService;

    @Autowired
    private DbCallbackExecutorService callbackExecutorService;

    @Autowired
    private TbApiUsageReportClient apiUsageReportClient;

    @Autowired
    private DefaultTbDeviceProfileCache deviceProfileCache;

    @Autowired
    private DefaultTbAssetProfileCache assetProfileCache;

    private ExecutorService callbackExecutor;

    private final Gson gson = new Gson();

    protected TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> integrationRuleEngineMsgProducer;

    @PostConstruct
    public void init() {
        super.init();
        integrationRuleEngineMsgProducer = producerProvider.getIntegrationRuleEngineMsgProducer();
        this.callbackExecutor = ThingsBoardExecutors.newWorkStealingPool(20, "default-integration-callback");
    }

    @PreDestroy
    public void destroy() {
        if (callbackExecutor != null) {
            callbackExecutor.shutdownNow();
        }
    }

    @Override
    public Runnable processUplinkData(AbstractIntegration integration, DeviceUplinkDataProto data, IntegrationCallback<Void> callback) {
        return processUplinkData(integration, integration.getId().getId(), data, callback); //for local integration context sessionId is exact integrationId
    }

    @Override
    public Runnable processUplinkData(AbstractIntegration integration, UUID sessionId, DeviceUplinkDataProto data, IntegrationCallback<Void> callback) {
        Device device = processGetOrCreateDevice(integration, data.getDeviceName(), data.getDeviceType(), data.getDeviceLabel(), data.getCustomerName(), data.getGroupName());
        TransportProtos.SessionInfoProto sessionInfo = ProtoUtils.toSessionInfo(sessionId, device);

        return () -> {
            boolean hasTelemetry = data.hasPostTelemetryMsg();
            boolean hasAttributes = data.hasPostAttributesMsg();

            if (!hasTelemetry && !hasAttributes) {
                callback.onSuccess(null);
                return;
            }

            int operationCount = (hasTelemetry ? 1 : 0) + (hasAttributes ? 1 : 0);
            IntegrationCallback<Void> compositeCallback = operationCount > 1 ? new CompositeCallback(operationCount, callback) : callback;

            if (hasTelemetry) {
                process(sessionInfo, data.getPostTelemetryMsg(), compositeCallback);
            }

            if (hasAttributes) {
                process(sessionInfo, data.getPostAttributesMsg(), compositeCallback);
            }
        };
    }

    @Override
    public Runnable processUplinkData(AbstractIntegration configuration, AssetUplinkDataProto data, IntegrationCallback<Void> callback) {
        Asset asset = processGetOrCreateAsset(configuration, data.getAssetName(), data.getAssetType(), data.getAssetLabel(), data.getCustomerName(), data.getGroupName());

        return () -> {
            boolean hasTelemetry = data.hasPostTelemetryMsg();
            boolean hasAttributes = data.hasPostAttributesMsg();

            if (!hasTelemetry && !hasAttributes) {
                callback.onSuccess(null);
                return;
            }

            int operationCount = (hasTelemetry ? 1 : 0) + (hasAttributes ? 1 : 0);
            IntegrationCallback<Void> compositeCallback = operationCount > 1 ? new CompositeCallback(operationCount, callback) : callback;

            if (hasTelemetry) {
                process(asset, data.getPostTelemetryMsg(), compositeCallback);
            }

            if (hasAttributes) {
                process(asset, data.getPostAttributesMsg(), compositeCallback);
            }
        };
    }

    @Override
    public Runnable processUplinkData(AbstractIntegration integrationInfo, EntityViewDataProto data, IntegrationCallback<Void> callback) {
        Device device = processGetOrCreateDevice(integrationInfo, data.getDeviceName(), data.getDeviceType(), null, null, null);
        getOrCreateEntityView(integrationInfo, device, data);
        return () -> callback.onSuccess(null);
    }

    @Override
    public void processUplinkData(AbstractIntegration info, TbMsg data, IntegrationApiCallback callback) {
        process(info.getTenantId(), data, callback);
    }

    @Override
    public void processUplinkData(TbIntegrationEventProto data, IntegrationApiCallback callback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(data.getTenantIdMSB(), data.getTenantIdLSB()));
        var eventSource = data.getSource();
        EntityId entityid = null;
        switch (eventSource) {
            case DEVICE:
                Device device = deviceService.findDeviceByTenantIdAndName(tenantId, data.getDeviceName());
                if (device != null) {
                    entityid = device.getId();
                }
                break;
            case INTEGRATION:
                entityid = new IntegrationId(new UUID(data.getEventSourceIdMSB(), data.getEventSourceIdLSB()));
                break;
            case UPLINK_CONVERTER:
            case DOWNLINK_CONVERTER:
                entityid = new ConverterId(new UUID(data.getEventSourceIdMSB(), data.getEventSourceIdLSB()));
                break;
        }
        if (entityid != null) {
            saveEvent(tenantId, entityid, data, callback);
        } else {
            callback.onSuccess(null);
        }
    }

    @Override
    public void processUplinkData(TbIntegrationTsDataProto data, IntegrationApiCallback integrationApiCallback) {
        TenantId tenantId = TenantId.fromUUID(new UUID(data.getTenantIdMSB(), data.getTenantIdLSB()));
        var eventSource = data.getSource();
        EntityId entityid = switch (eventSource) {
            case INTEGRATION -> new IntegrationId(new UUID(data.getEntityIdMSB(), data.getEntityIdLSB()));
            case UPLINK_CONVERTER, DOWNLINK_CONVERTER -> new ConverterId(new UUID(data.getEntityIdMSB(), data.getEntityIdLSB()));
            default -> throw new RuntimeException("Not supported!");
        };

        List<TsKvEntry> statistics = KvProtoUtil.fromTsValueProtoList(data.getTsDataList());
        telemetrySubscriptionService.saveTimeseriesInternal(TimeseriesSaveRequest.builder()
                .tenantId(tenantId)
                .entityId(entityid)
                .entries(statistics)
                .callback(new FutureCallback<>() {
                    @Override
                    public void onSuccess(Void result) {
                        log.trace("[{}] Persisted statistics telemetry: {}", entityid, statistics);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        log.warn("[{}] Failed to persist statistics telemetry: {}", entityid, statistics, t);
                    }
                })
                .build());
    }

    @Override
    public void processDeviceActivityData(DeviceActivityProto data, IntegrationApiCallback callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(data.getTenantIdMSB(), data.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(data.getDeviceIdMSB(), data.getDeviceIdLSB()));
            onActivity(new IntegrationActivityKey(tenantId, deviceId), null, getCurrentTimeMillis());
            callback.onSuccess(null);
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private void saveEvent(TenantId tenantId, EntityId entityId, TbIntegrationEventProto proto, IntegrationApiCallback callback) {
        try {
            Event event = JavaSerDesUtil.decode(proto.getEvent().toByteArray());
            event.setTenantId(tenantId);
            event.setEntityId(entityId.getId());

            ListenableFuture<Void> saveEventFuture = eventService.saveAsync(event);

            if (entityId.getEntityType().equals(EntityType.INTEGRATION) && event.getType().equals(EventType.LC_EVENT)) {
                LifecycleEvent lcEvent = (LifecycleEvent) event;

                String key = "integration_status_" + event.getServiceId().toLowerCase();

                if (lcEvent.getLcEventType().equals("STARTED") || lcEvent.getLcEventType().equals("UPDATED")) {
                    ObjectNode value = JacksonUtil.newObjectNode();

                    if (lcEvent.isSuccess()) {
                        value.put("success", true);
                    } else {
                        value.put("success", false);
                        value.put("serviceId", lcEvent.getServiceId());
                        value.put("error", lcEvent.getError());
                    }

                    AttributeKvEntry attr = new BaseAttributeKvEntry(new JsonDataEntry(key, JacksonUtil.toString(value)), event.getCreatedTime());

                    saveEventFuture = Futures.transformAsync(saveEventFuture, _ ->
                                    Futures.transform(attributesService.save(tenantId, entityId, AttributeScope.SERVER_SCOPE, Collections.singletonList(attr)),
                                            _ -> null, MoreExecutors.directExecutor()),
                            MoreExecutors.directExecutor());
                } else if (lcEvent.getLcEventType().equals("STOPPED")) {
                    saveEventFuture = Futures.transformAsync(saveEventFuture, _ ->
                                    Futures.transform(attributesService.removeAll(tenantId, entityId, AttributeScope.SERVER_SCOPE, Collections.singletonList(key)),
                                            _ -> null, MoreExecutors.directExecutor()),
                            MoreExecutors.directExecutor());
                }
            }

            DonAsynchron.withCallback(saveEventFuture, callback::onSuccess, callback::onError);
        } catch (Exception t) {
            log.error("[{}][{}][{}] Failed to save event!", tenantId, entityId, proto.getEvent(), t);
            callback.onError(t);
            throw t;
        }
    }

    @Override
    public void process(SessionInfoProto sessionInfo, PostTelemetryMsg msg, IntegrationCallback<Void> callback) {
        try {
            if (msg.getTsKvListCount() == 0) {
                callback.onSuccess(null);
                return;
            }
            TenantId tenantId = TenantId.fromUUID(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            onActivity(new IntegrationActivityKey(tenantId, deviceId), null, getCurrentTimeMillis());
            int dataPoints = 0;
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                dataPoints += tsKv.getKvCount();
            }
            MsgPackCallback packCallback = new MsgPackCallback(msg.getTsKvListCount(), new ApiStatsProxyCallback<>(tenantId, getCustomerId(sessionInfo), dataPoints, callback));
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                TbMsgMetaData metaData = new TbMsgMetaData();
                metaData.putValue("deviceName", sessionInfo.getDeviceName());
                metaData.putValue("deviceType", sessionInfo.getDeviceType());
                metaData.putValue("ts", tsKv.getTs() + "");
                JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
                sendToRuleEngine(tenantId, deviceId, sessionInfo, json, metaData, TbMsgType.POST_TELEMETRY_REQUEST, packCallback);
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public void process(SessionInfoProto sessionInfo, PostAttributeMsg msg, IntegrationCallback<Void> callback) {
        try {
            TenantId tenantId = TenantId.fromUUID(new UUID(sessionInfo.getTenantIdMSB(), sessionInfo.getTenantIdLSB()));
            DeviceId deviceId = new DeviceId(new UUID(sessionInfo.getDeviceIdMSB(), sessionInfo.getDeviceIdLSB()));
            onActivity(new IntegrationActivityKey(tenantId, deviceId), null, getCurrentTimeMillis());
            JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("deviceName", sessionInfo.getDeviceName());
            metaData.putValue("deviceType", sessionInfo.getDeviceType());
            sendToRuleEngine(tenantId, deviceId, sessionInfo, json, metaData, TbMsgType.POST_ATTRIBUTES_REQUEST,
                    new IntegrationTbQueueCallback(new ApiStatsProxyCallback<>(tenantId, getCustomerId(sessionInfo), msg.getKvList().size(), callback)));
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    @Override
    public void process(TenantId tenantId, TbMsg tbMsg, IntegrationCallback<Void> callback) {
        sendToRuleEngine(tenantId, tbMsg, new IntegrationTbQueueCallback(new ApiStatsProxyCallback<>(tenantId, tbMsg.getCustomerId(), 1, callback)));
    }

    @Override
    public Device processGetOrCreateDevice(AbstractIntegration integration, String name, String type, String label, String customerName, String groupName) {
        Device device = deviceService.findDeviceByTenantIdAndName(integration.getTenantId(), name);
        if (device != null) {
            return device;
        }
        RefCountedLock ref = entityCreationLocks.compute(integration.getTenantId(), (_, v) -> {
            if (v == null) {
                v = new RefCountedLock();
            }
            v.refCount++;
            return v;
        });
        ref.lock.lock();
        try {
            device = deviceService.findDeviceByTenantIdAndName(integration.getTenantId(), name);
            if (device == null) {
                if (!integration.isAllowCreateDevicesOrAssets()) {
                    throw new ThingsboardRuntimeException("Creating devices is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
                }
                device = new Device();
                device.setName(name);
                device.setType(type);
                device.setTenantId(integration.getTenantId());
                if (!StringUtils.isEmpty(label)) {
                    device.setLabel(label);
                }
                if (!StringUtils.isEmpty(customerName)) {
                    Customer customer = getOrCreateCustomer(integration, customerName);
                    device.setCustomerId(customer.getId());
                }

                device = deviceService.saveDevice(device);

                if (!StringUtils.isEmpty(groupName)) {
                    addEntityToEntityGroup(groupName, integration, device.getId(), device.getOwnerId(), device.getEntityType());
                }

                createRelationFromIntegration(integration, device.getId());
                pushDeviceCreatedEventToRuleEngine(integration, device);
            }
            return device;
        } finally {
            ref.lock.unlock();
            entityCreationLocks.compute(integration.getTenantId(), (_, v) -> {
                if (v == null) {
                    return null;
                }
                v.refCount--;
                return v.refCount <= 0 ? null : v;
            });
        }
    }

    @Override
    public Asset processGetOrCreateAsset(AbstractIntegration integration, String name, String type, String label, String customerName, String groupName) {
        Asset asset = assetService.findAssetByTenantIdAndName(integration.getTenantId(), name);
        if (asset != null) {
            return asset;
        }
        RefCountedLock ref = entityCreationLocks.compute(integration.getTenantId(), (k, v) -> {
            if (v == null) {
                v = new RefCountedLock();
            }
            v.refCount++;
            return v;
        });
        ref.lock.lock();
        try {
            asset = assetService.findAssetByTenantIdAndName(integration.getTenantId(), name);
            if (asset == null) {
                if (!integration.isAllowCreateDevicesOrAssets()) {
                    throw new ThingsboardRuntimeException("Creating assets is forbidden!", ThingsboardErrorCode.PERMISSION_DENIED);
                }
                asset = new Asset();
                asset.setName(name);
                asset.setType(type);
                asset.setTenantId(integration.getTenantId());
                if (!StringUtils.isEmpty(label)) {
                    asset.setLabel(label);
                }
                if (!StringUtils.isEmpty(customerName)) {
                    Customer customer = getOrCreateCustomer(integration, customerName);
                    asset.setCustomerId(customer.getId());
                }
                asset = assetService.saveAsset(asset);

                if (!StringUtils.isEmpty(groupName)) {
                    addEntityToEntityGroup(groupName, integration, asset.getId(), asset.getOwnerId(), asset.getEntityType());
                }

                createRelationFromIntegration(integration, asset.getId());
                pushAssetCreatedEventToRuleEngine(integration, asset);
            }
            return asset;
        } finally {
            ref.lock.unlock();
            entityCreationLocks.compute(integration.getTenantId(), (k, v) -> {
                if (v == null) {
                    return null;
                }
                v.refCount--;
                return v.refCount <= 0 ? null : v;
            });
        }
    }

    private EntityView getOrCreateEntityView(AbstractIntegration configuration, Device device, EntityViewDataProto proto) {
        String entityViewName = proto.getViewName();
        EntityView entityView = entityViewService.findEntityViewByTenantIdAndName(configuration.getTenantId(), entityViewName);
        if (entityView == null) {
            RefCountedLock ref = entityCreationLocks.compute(configuration.getTenantId(), (k, v) -> {
                if (v == null) {
                    v = new RefCountedLock();
                }
                v.refCount++;
                return v;
            });
            ref.lock.lock();
            try {
                entityView = entityViewService.findEntityViewByTenantIdAndName(configuration.getTenantId(), entityViewName);
                if (entityView == null) {
                    entityView = new EntityView();
                    entityView.setName(entityViewName);
                    entityView.setType(proto.getViewType());
                    entityView.setTenantId(configuration.getTenantId());
                    entityView.setEntityId(device.getId());

                    TelemetryEntityView telemetryEntityView = new TelemetryEntityView();
                    telemetryEntityView.setTimeseries(proto.getTelemetryKeysList());
                    entityView.setKeys(telemetryEntityView);

                    entityView = entityViewService.saveEntityView(entityView);
                    createRelationFromIntegration(configuration, entityView.getId());
                }
            } finally {
                ref.lock.unlock();
                entityCreationLocks.compute(configuration.getTenantId(), (k, v) -> {
                    if (v == null) {
                        return null;
                    }
                    v.refCount--;
                    return v.refCount <= 0 ? null : v;
                });
            }
        }
        return entityView;
    }

    private Customer getOrCreateCustomer(AbstractIntegration integration, String customerName) {
        Customer customer;
        Optional<Customer> customerOptional = customerService.findCustomerByTenantIdAndTitle(integration.getTenantId(), customerName);
        if (customerOptional.isPresent()) {
            customer = customerOptional.get();
        } else {
            customer = new Customer();
            customer.setTitle(customerName);
            customer.setTenantId(integration.getTenantId());
            customer = customerService.saveCustomer(customer);
            pushCustomerCreatedEventToRuleEngine(integration, customer);
        }
        return customer;
    }

    private void addEntityToEntityGroup(String groupName, AbstractIntegration integration, EntityId entityId, EntityId parentId, EntityType entityType) {
        TenantId tenantId = integration.getTenantId();
        ListenableFuture<Optional<EntityGroup>> futureEntityGroup = entityGroupService
                .findEntityGroupByTypeAndNameAsync(tenantId, parentId, entityType, groupName);

        DonAsynchron.withCallback(futureEntityGroup, optionalEntityGroup -> {
            EntityGroup entityGroup =
                    optionalEntityGroup.orElseGet(() -> createEntityGroup(groupName, parentId, entityType, tenantId));
            pushEntityGroupCreatedEventToRuleEngine(integration, entityGroup);
            entityGroupService.addEntityToEntityGroup(tenantId, entityGroup.getId(), entityId);
        }, throwable -> log.warn("[{}][{}] Failed to find entity group: {}:{}", tenantId, parentId, entityType, groupName, throwable), callbackExecutorService);
    }

    private EntityGroup createEntityGroup(String entityGroupName, EntityId parentEntityId, EntityType entityType, TenantId tenantId) {
        EntityGroup entityGroup = new EntityGroup();
        entityGroup.setName(entityGroupName);
        entityGroup.setType(entityType);
        return entityGroupService.saveEntityGroup(tenantId, parentEntityId, entityGroup);
    }

    private void createRelationFromIntegration(AbstractIntegration integration, EntityId entityId) {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(integration.getId());
        relation.setTo(entityId);
        relation.setTypeGroup(RelationTypeGroup.COMMON);
        relation.setType(EntityRelation.INTEGRATION_TYPE);
        relationService.saveRelation(integration.getTenantId(), relation);
    }

    private void pushDeviceCreatedEventToRuleEngine(AbstractIntegration integration, Device device) {
        try {
            DeviceProfile deviceProfile = deviceProfileCache.find(device.getDeviceProfileId());
            RuleChainId ruleChainId;
            String queueName;

            if (deviceProfile == null) {
                ruleChainId = null;
                queueName = null;
            } else {
                ruleChainId = deviceProfile.getDefaultRuleChainId();
                queueName = deviceProfile.getDefaultQueueName();
            }

            JsonNode entityNode = JacksonUtil.valueToTree(device);
            TbMsg tbMsg = TbMsg.newMsg()
                    .queueName(queueName)
                    .type(TbMsgType.ENTITY_CREATED)
                    .originator(device.getId())
                    .metaData(deviceActionTbMsgMetaData(integration, device))
                    .data(JacksonUtil.toString(entityNode))
                    .ruleChainId(ruleChainId)
                    .build();

            process(device.getTenantId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push device action to rule engine: {}", device.getId(), TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    private void pushAssetCreatedEventToRuleEngine(AbstractIntegration integration, Asset asset) {
        try {
            AssetProfile assetProfile = assetProfileCache.find(asset.getAssetProfileId());
            RuleChainId ruleChainId;
            String queueName;
            if (assetProfile == null) {
                ruleChainId = null;
                queueName = null;
            } else {
                ruleChainId = assetProfile.getDefaultRuleChainId();
                queueName = assetProfile.getDefaultQueueName();
            }
            JsonNode entityNode = JacksonUtil.valueToTree(asset);
            TbMsg tbMsg = TbMsg.newMsg()
                    .queueName(queueName)
                    .type(TbMsgType.ENTITY_CREATED)
                    .originator(asset.getId())
                    .customerId(asset.getCustomerId())
                    .copyMetaData(assetActionTbMsgMetaData(integration, asset))
                    .data(JacksonUtil.toString(entityNode))
                    .ruleChainId(ruleChainId)
                    .build();
            process(integration.getTenantId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push asset action to rule engine: {}", asset.getId(), TbMsgType.ENTITY_CREATED.name(), e);
        }
    }


    private void pushEntityGroupCreatedEventToRuleEngine(AbstractIntegration integration, EntityGroup entityGroup) {
        try {
            JsonNode entityNode = JacksonUtil.valueToTree(entityGroup);
            TbMsg tbMsg = TbMsg.newMsg()
                    .type(TbMsgType.ENTITY_CREATED)
                    .originator(entityGroup.getId())
                    .copyMetaData(getTbMsgMetaData(integration))
                    .data(JacksonUtil.toString(entityNode))
                    .build();
            process(integration.getTenantId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push entityGroup action to rule engine: {}", entityGroup.getId(), TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    private void pushCustomerCreatedEventToRuleEngine(AbstractIntegration integration, Customer customer) {
        try {
            JsonNode entityNode = JacksonUtil.valueToTree(customer);
            TbMsg tbMsg = TbMsg.newMsg()
                    .type(TbMsgType.ENTITY_CREATED)
                    .originator(customer.getId())
                    .customerId(customer.getParentCustomerId())
                    .copyMetaData(getTbMsgMetaData(integration))
                    .data(JacksonUtil.toString(entityNode))
                    .build();
            process(customer.getTenantId(), tbMsg, null);
        } catch (IllegalArgumentException e) {
            log.warn("[{}] Failed to push customer action to rule engine: {}", customer.getId(), TbMsgType.ENTITY_CREATED.name(), e);
        }
    }

    private TbMsgMetaData deviceActionTbMsgMetaData(AbstractIntegration integration, Device device) {
        return getActionTbMsgMetaData(integration, device.getCustomerId());
    }

    private TbMsgMetaData assetActionTbMsgMetaData(AbstractIntegration integration, Asset asset) {
        return getActionTbMsgMetaData(integration, asset.getCustomerId());
    }

    private TbMsgMetaData getActionTbMsgMetaData(AbstractIntegration integration, CustomerId customerId) {
        TbMsgMetaData metaData = getTbMsgMetaData(integration);
        if (customerId != null && !customerId.isNullUid()) {
            metaData.putValue("customerId", customerId.toString());
        }
        return metaData;
    }

    private TbMsgMetaData getTbMsgMetaData(AbstractIntegration integration) {
        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("integrationId", integration.getId().toString());
        metaData.putValue("integrationName", integration.getName());
        return metaData;
    }

    private void process(Asset asset, PostTelemetryMsg msg, IntegrationCallback<Void> callback) {
        try {
            if (msg.getTsKvListCount() == 0) {
                callback.onSuccess(null);
                return;
            }
            int dataPoints = 0;
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                dataPoints += tsKv.getKvCount();
            }
            MsgPackCallback packCallback = new MsgPackCallback(msg.getTsKvListCount(), new ApiStatsProxyCallback<>(asset.getTenantId(), asset.getCustomerId(), dataPoints, callback));
            for (TransportProtos.TsKvListProto tsKv : msg.getTsKvListList()) {
                TbMsgMetaData metaData = new TbMsgMetaData();
                metaData.putValue("assetName", asset.getName());
                metaData.putValue("assetType", asset.getType());
                metaData.putValue("ts", tsKv.getTs() + "");
                JsonObject json = JsonUtils.getJsonObject(tsKv.getKvList());
                sendToRuleEngine(asset.getTenantId(), asset.getId(), asset.getAssetProfileId(),
                        asset.getCustomerId(), json, metaData, TbMsgType.POST_TELEMETRY_REQUEST, packCallback);
            }
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    private void process(Asset asset, PostAttributeMsg msg, IntegrationCallback<Void> callback) {
        try {
            JsonObject json = JsonUtils.getJsonObject(msg.getKvList());
            TbMsgMetaData metaData = new TbMsgMetaData();
            metaData.putValue("assetName", asset.getName());
            metaData.putValue("assetType", asset.getType());
            sendToRuleEngine(asset.getTenantId(), asset.getId(), asset.getAssetProfileId(),
                    asset.getCustomerId(), json, metaData, TbMsgType.POST_ATTRIBUTES_REQUEST,
                    new IntegrationTbQueueCallback(new ApiStatsProxyCallback<>(asset.getTenantId(), asset.getCustomerId(), msg.getKvList().size(), callback)));
        } catch (Exception e) {
            callback.onError(e);
        }
    }

    void sendToRuleEngine(TenantId tenantId, DeviceId deviceId, TransportProtos.SessionInfoProto sessionInfo, JsonObject json,
                          TbMsgMetaData metaData, TbMsgType msgType, TbQueueCallback callback) {
        DeviceProfileId deviceProfileId = new DeviceProfileId(new UUID(sessionInfo.getDeviceProfileIdMSB(), sessionInfo.getDeviceProfileIdLSB()));

        DeviceProfile deviceProfile = deviceProfileCache.get(tenantId, deviceProfileId);
        RuleChainId ruleChainId;
        String queueName;

        if (deviceProfile == null) {
            log.warn("[{}] Device profile is null!", deviceProfileId);
            ruleChainId = null;
            queueName = null;
        } else {
            ruleChainId = deviceProfile.getDefaultRuleChainId();
            queueName = deviceProfile.getDefaultQueueName();
        }

        TbMsg tbMsg = TbMsg.newMsg()
                .queueName(queueName)
                .type(msgType)
                .originator(deviceId)
                .customerId(getCustomerId(sessionInfo))
                .copyMetaData(metaData)
                .data(gson.toJson(json))
                .ruleChainId(ruleChainId)
                .build();
        sendToRuleEngine(tenantId, tbMsg, callback);
    }

    private void sendToRuleEngine(TenantId tenantId, AssetId assetId, AssetProfileId assetProfileId, CustomerId customerId, JsonObject json,
                                  TbMsgMetaData metaData, TbMsgType msgType, TbQueueCallback callback) {
        AssetProfile assetProfile = assetProfileCache.get(tenantId, assetProfileId);
        RuleChainId ruleChainId;
        String queueName;

        if (assetProfile == null) {
            log.warn("[{}] Asset profile is null!", assetProfileId);
            ruleChainId = null;
            queueName = null;
        } else {
            ruleChainId = assetProfile.getDefaultRuleChainId();
            queueName = assetProfile.getDefaultQueueName();
        }

        TbMsg tbMsg = TbMsg.newMsg()
                .queueName(queueName)
                .type(msgType)
                .originator(assetId)
                .customerId(customerId)
                .copyMetaData(metaData)
                .data(gson.toJson(json))
                .ruleChainId(ruleChainId)
                .build();
        sendToRuleEngine(tenantId, tbMsg, callback);
    }

    private void sendToRuleEngine(TenantId tenantId, TbMsg tbMsg, TbQueueCallback callback) {
        ruleEngineProducerService.sendToRuleEngine(integrationRuleEngineMsgProducer, tenantId, tbMsg, callback);
    }

    private class IntegrationTbQueueCallback implements TbQueueCallback {

        private final IntegrationCallback<Void> callback;

        private IntegrationTbQueueCallback(IntegrationCallback<Void> callback) {
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            try {
                DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to submit success callback", e);
                try {
                    if (callback != null) {
                        callback.onError(e);
                    }
                } catch (Exception inner) {
                    log.warn("Failed to invoke failure callback directly", inner);
                }
            }
        }

        @Override
        public void onFailure(Throwable t) {
            try {
                DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> {
                    if (callback != null) {
                        callback.onError(t);
                    }
                });
            } catch (Exception e) {
                log.warn("Failed to submit failure callback for error: {}", t.getMessage(), e);
                try {
                    if (callback != null) {
                        callback.onError(t);
                    }
                } catch (Exception inner) {
                    log.warn("Failed to invoke failure callback directly", inner);
                }
            }
        }

    }

    private class MsgPackCallback implements TbQueueCallback {

        private final AtomicInteger msgCount;
        private final AtomicBoolean completed = new AtomicBoolean(false);
        private final IntegrationCallback<Void> callback;

        public MsgPackCallback(Integer msgCount, IntegrationCallback<Void> callback) {
            this.msgCount = new AtomicInteger(msgCount);
            this.callback = callback;
        }

        @Override
        public void onSuccess(TbQueueMsgMetadata metadata) {
            if (msgCount.decrementAndGet() <= 0 && completed.compareAndSet(false, true) && callback != null) {
                try {
                    DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> callback.onSuccess(null));
                } catch (Exception e) {
                    log.warn("Failed to submit success callback", e);
                    try {
                        callback.onError(e);
                    } catch (Exception inner) {
                        log.warn("Failed to invoke failure callback directly", inner);
                    }
                }
            }
        }

        @Override
        public void onFailure(Throwable t) {
            if (completed.compareAndSet(false, true) && callback != null) {
                try {
                    DefaultPlatformIntegrationService.this.callbackExecutor.submit(() -> callback.onError(t));
                } catch (Exception e) {
                    log.warn("Failed to submit failure callback for error: {}", t.getMessage(), e);
                    try {
                        callback.onError(t);
                    } catch (Exception inner) {
                        log.warn("Failed to invoke failure callback directly", inner);
                    }
                }
            }
        }

    }

    private static class CompositeCallback implements IntegrationCallback<Void> {

        private final AtomicInteger pendingCount;
        private final IntegrationCallback<Void> callback;
        private final AtomicBoolean completed = new AtomicBoolean(false);

        public CompositeCallback(int operationCount, IntegrationCallback<Void> callback) {
            this.pendingCount = new AtomicInteger(operationCount);
            this.callback = callback;
        }

        @Override
        public void onSuccess(Void msg) {
            if (pendingCount.decrementAndGet() <= 0 && completed.compareAndSet(false, true) && callback != null) {
                callback.onSuccess(null);
            }
        }

        @Override
        public void onError(Throwable e) {
            if (completed.compareAndSet(false, true) && callback != null) {
                callback.onError(e);
            }
        }

    }

    private class ApiStatsProxyCallback<T> implements IntegrationCallback<T> {

        private final TenantId tenantId;
        private final CustomerId customerId;
        private final int dataPoints;
        private final IntegrationCallback<T> callback;

        public ApiStatsProxyCallback(TenantId tenantId, CustomerId customerId, int dataPoints, IntegrationCallback<T> callback) {
            this.tenantId = tenantId;
            this.customerId = customerId;
            this.dataPoints = dataPoints;
            this.callback = callback;
        }

        @Override
        public void onSuccess(T msg) {
            try {
                apiUsageReportClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_MSG_COUNT, 1);
                apiUsageReportClient.report(tenantId, customerId, ApiUsageRecordKey.TRANSPORT_DP_COUNT, dataPoints);
            } finally {
                if (callback != null) {
                    callback.onSuccess(msg);
                }
            }
        }

        @Override
        public void onError(Throwable e) {
            if (callback != null) {
                callback.onError(e);
            }
        }

    }

    private static CustomerId getCustomerId(SessionInfoProto sessionInfo) {
        if (sessionInfo.getCustomerIdMSB() != 0 && sessionInfo.getCustomerIdLSB() != 0) {
            return new CustomerId(new UUID(sessionInfo.getCustomerIdMSB(), sessionInfo.getCustomerIdLSB()));
        }
        return null;
    }

    // Wraps a lock with a ref count so we can safely remove it from the map when no one needs it.
    // Plain computeIfAbsent + lock() has a gap where cleanup can remove the lock before it's held,
    // causing two threads to lock different instances and breaking mutual exclusion.
    private static class RefCountedLock {
        final ReentrantLock lock = new ReentrantLock();
        int refCount = 0; // only accessed under ConcurrentHashMap.compute()

    }

}
