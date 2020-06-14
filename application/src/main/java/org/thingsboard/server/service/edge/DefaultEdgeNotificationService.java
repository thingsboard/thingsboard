package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.Event;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeQueueEntityType;
import org.thingsboard.server.common.data.edge.EdgeQueueEntry;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.TimePageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainMetaData;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.TbCallback;
import org.thingsboard.server.common.msg.session.SessionMsgType;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.util.TbCoreComponent;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
@TbCoreComponent
@Slf4j
public class DefaultEdgeNotificationService implements EdgeNotificationService {

    private static final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private EntityViewService entityViewService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private RelationService relationService;

    @Autowired
    private EventService eventService;

    private ExecutorService tsCallBackExecutor;

    @PostConstruct
    public void initExecutor() {
        tsCallBackExecutor = Executors.newSingleThreadExecutor();
    }

    @PreDestroy
    public void shutdownExecutor() {
        if (tsCallBackExecutor != null) {
            tsCallBackExecutor.shutdownNow();
        }
    }

    @Override
    public TimePageData<Event> findQueueEvents(TenantId tenantId, EdgeId edgeId, TimePageLink pageLink) {
        return eventService.findEvents(tenantId, edgeId, DataConstants.EDGE_QUEUE_EVENT_TYPE, pageLink);
    }

    @Override
    public Edge setEdgeRootRuleChain(TenantId tenantId, Edge edge, RuleChainId ruleChainId) throws IOException {
        edge.setRootRuleChainId(ruleChainId);
        Edge savedEdge = edgeService.saveEdge(edge);
        RuleChain ruleChain = ruleChainService.findRuleChainById(tenantId, ruleChainId);
        saveEventToEdgeQueue(tenantId, edge.getId(), EdgeQueueEntityType.RULE_CHAIN, DataConstants.ENTITY_UPDATED, mapper.writeValueAsString(ruleChain), new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void aVoid) {
                log.debug("Event saved successfully!");
            }

            @Override
            public void onFailure(Throwable t) {
                log.debug("Failure during event save", t);
            }
        });
        return savedEdge;
    }

    private void saveEventToEdgeQueue(TenantId tenantId, EdgeId edgeId, EdgeQueueEntityType entityType, String type, String data, FutureCallback<Void> callback) throws IOException {
        log.debug("Pushing single event to edge queue. tenantId [{}], edgeId [{}], entityType [{}], type[{}], data [{}]", tenantId, edgeId, entityType, type, data);

        EdgeQueueEntry queueEntry = new EdgeQueueEntry();
        queueEntry.setEntityType(entityType);
        queueEntry.setType(type);
        queueEntry.setData(data);

        Event event = new Event();
        event.setEntityId(edgeId);
        event.setTenantId(tenantId);
        event.setType(DataConstants.EDGE_QUEUE_EVENT_TYPE);
        event.setBody(mapper.valueToTree(queueEntry));
        ListenableFuture<Event> saveFuture = eventService.saveAsync(event);

        addMainCallback(saveFuture, callback);
    }

    private void addMainCallback(ListenableFuture<Event> saveFuture, final FutureCallback<Void> callback) {
        Futures.addCallback(saveFuture, new FutureCallback<Event>() {
            @Override
            public void onSuccess(@Nullable Event result) {
                callback.onSuccess(null);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onFailure(t);
            }
        }, tsCallBackExecutor);
    }

    @Override
    public void pushNotificationToEdge(TransportProtos.EdgeNotificationMsgProto edgeNotificationMsg, TbCallback callback) {
        if (tbMsg.getType().equals(SessionMsgType.POST_TELEMETRY_REQUEST.name()) ||
                tbMsg.getType().equals(SessionMsgType.POST_ATTRIBUTES_REQUEST.name()) ||
                tbMsg.getType().equals(DataConstants.ATTRIBUTES_UPDATED) ||
                tbMsg.getType().equals(DataConstants.ATTRIBUTES_DELETED)) {
            processCustomTbMsg(tenantId, tbMsg, callback);
        } else {
            try {
                switch (tbMsg.getOriginator().getEntityType()) {
                    case EDGE:
                        processEdge(tenantId, tbMsg, callback);
                        break;
                    case ASSET:
                        processAsset(tenantId, tbMsg, callback);
                        break;
                    case DEVICE:
                        processDevice(tenantId, tbMsg, callback);
                        break;
                    case DASHBOARD:
                        processDashboard(tenantId, tbMsg, callback);
                        break;
                    case RULE_CHAIN:
                        processRuleChain(tenantId, tbMsg, callback);
                        break;
                    case ENTITY_VIEW:
                        processEntityView(tenantId, tbMsg, callback);
                        break;
                    case ALARM:
                        processAlarm(tenantId, tbMsg, callback);
                        break;
                    default:
                        log.debug("Entity type [{}] is not designed to be pushed to edge", tbMsg.getOriginator().getEntityType());
                }
            } catch (IOException e) {
                log.error("Can't push to edge updates, entity type [{}], data [{}]", tbMsg.getOriginator().getEntityType(), tbMsg.getData(), e);
            }
        }
    }


    private void processCustomTbMsg(TenantId tenantId, TbMsg tbMsg, FutureCallback<Void> callback) {
        ListenableFuture<EdgeId> edgeIdFuture = getEdgeIdByOriginatorId(tenantId, tbMsg.getOriginator());
        Futures.transform(edgeIdFuture, edgeId -> {
            EdgeQueueEntityType edgeQueueEntityType = getEdgeQueueTypeByEntityType(tbMsg.getOriginator().getEntityType());
            if (edgeId != null && edgeQueueEntityType != null) {
                try {
                    saveEventToEdgeQueue(tenantId, edgeId, edgeQueueEntityType, tbMsg.getType(), Base64.encodeBase64String(TbMsg.toByteArray(tbMsg)), callback);
                } catch (IOException e) {
                    log.error("Error while saving custom tbMsg into Edge Queue", e);
                }
            }
            return null;
        }, MoreExecutors.directExecutor());
    }

    private void processDevice(TenantId tenantId, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
            case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
                processAssignedEntity(tenantId, tbMsg, EdgeQueueEntityType.DEVICE, callback);
                break;
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_UPDATED:
                Device device = mapper.readValue(tbMsg.getData(), Device.class);
                pushEventToEdge(tenantId, device.getId(), EdgeQueueEntityType.DEVICE, tbMsg, callback);
                break;
            default:
                log.warn("Unsupported msgType [{}], tbMsg [{}]", tbMsg.getType(), tbMsg);
        }
    }

    private void processEdge(TenantId tenantId, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_UPDATED:
                // TODO: voba - handle properly edge creation
                break;
            default:
                log.warn("Unsupported msgType [{}], tbMsg [{}]", tbMsg.getType(), tbMsg);
        }
    }

    private void processAsset(TenantId tenantId, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
            case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
                processAssignedEntity(tenantId, tbMsg, EdgeQueueEntityType.ASSET, callback);
                break;
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_UPDATED:
                Asset asset = mapper.readValue(tbMsg.getData(), Asset.class);
                pushEventToEdge(tenantId, asset.getId(), EdgeQueueEntityType.ASSET, tbMsg, callback);
                break;
            default:
                log.warn("Unsupported msgType [{}], tbMsg [{}]", tbMsg.getType(), tbMsg);
        }
    }

    private void processEntityView(TenantId tenantId, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
            case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
                processAssignedEntity(tenantId, tbMsg, EdgeQueueEntityType.ENTITY_VIEW, callback);
                break;
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_UPDATED:
                EntityView entityView = mapper.readValue(tbMsg.getData(), EntityView.class);
                pushEventToEdge(tenantId, entityView.getId(), EdgeQueueEntityType.ENTITY_VIEW, tbMsg, callback);
                break;
            default:
                log.warn("Unsupported msgType [{}], tbMsg [{}]", tbMsg.getType(), tbMsg);
        }
    }

    private void processAlarm(TenantId tenantId, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_UPDATED:
            case DataConstants.ALARM_ACK:
            case DataConstants.ALARM_CLEAR:
                Alarm alarm = mapper.readValue(tbMsg.getData(), Alarm.class);
                EdgeQueueEntityType edgeQueueEntityType = getEdgeQueueTypeByEntityType(alarm.getOriginator().getEntityType());
                if (edgeQueueEntityType != null) {
                    pushEventToEdge(tenantId, alarm.getOriginator(), EdgeQueueEntityType.ALARM, tbMsg, callback);
                }
                break;
            default:
                log.warn("Unsupported msgType [{}], tbMsg [{}]", tbMsg.getType(), tbMsg);
        }
    }

    private void processDashboard(TenantId tenantId, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
            case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
                processAssignedEntity(tenantId, tbMsg, EdgeQueueEntityType.DASHBOARD, callback);
                break;
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_UPDATED:
                Dashboard dashboard = mapper.readValue(tbMsg.getData(), Dashboard.class);
                ListenableFuture<TimePageData<Edge>> future = edgeService.findEdgesByTenantIdAndDashboardId(tenantId, dashboard.getId(), new TimePageLink(Integer.MAX_VALUE));
                Futures.transform(future, edges -> {
                    if (edges != null && edges.getData() != null && !edges.getData().isEmpty()) {
                        try {
                            for (Edge edge : edges.getData()) {
                                pushEventToEdge(tenantId, edge.getId(), EdgeQueueEntityType.DASHBOARD, tbMsg, callback);
                            }
                        } catch (IOException e) {
                            log.error("Can't push event to edge", e);
                        }
                    }
                    return null;
                }, MoreExecutors.directExecutor());
                break;
            default:
                log.warn("Unsupported msgType [{}], tbMsg [{}]", tbMsg.getType(), tbMsg);
        }
    }

    private void processRuleChain(TenantId tenantId, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
            case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
                processAssignedEntity(tenantId, tbMsg, EdgeQueueEntityType.RULE_CHAIN, callback);
                break;
            case DataConstants.ENTITY_DELETED:
            case DataConstants.ENTITY_CREATED:
            case DataConstants.ENTITY_UPDATED:
                RuleChain ruleChain = mapper.readValue(tbMsg.getData(), RuleChain.class);
                if (RuleChainType.EDGE.equals(ruleChain.getType())) {
                    ListenableFuture<TimePageData<Edge>> future = edgeService.findEdgesByTenantIdAndRuleChainId(tenantId, ruleChain.getId(), new TimePageLink(Integer.MAX_VALUE));
                    Futures.transform(future, edges -> {
                        if (edges != null && edges.getData() != null && !edges.getData().isEmpty()) {
                            try {
                                for (Edge edge : edges.getData()) {
                                    pushEventToEdge(tenantId, edge.getId(), EdgeQueueEntityType.RULE_CHAIN, tbMsg, callback);
                                }
                            } catch (IOException e) {
                                log.error("Can't push event to edge", e);
                            }
                        }
                        return null;
                    }, MoreExecutors.directExecutor());
                }
                break;
            default:
                log.warn("Unsupported msgType [{}], tbMsg [{}]", tbMsg.getType(), tbMsg);
        }
    }


    private void processAssignedEntity(TenantId tenantId, TbMsg tbMsg, EdgeQueueEntityType entityType, FutureCallback<Void> callback) throws IOException {
        EdgeId edgeId;
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
                edgeId = new EdgeId(UUID.fromString(tbMsg.getMetaData().getValue("assignedEdgeId")));
                pushEventToEdge(tenantId, edgeId, entityType, tbMsg, callback);
                break;
            case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
                edgeId = new EdgeId(UUID.fromString(tbMsg.getMetaData().getValue("unassignedEdgeId")));
                pushEventToEdge(tenantId, edgeId, entityType, tbMsg, callback);
                break;

        }
    }


    private void pushEventToEdge(TenantId tenantId, EntityId originatorId, EdgeQueueEntityType edgeQueueEntityType, TbMsg tbMsg, FutureCallback<Void> callback) {
        ListenableFuture<EdgeId> edgeIdFuture = getEdgeIdByOriginatorId(tenantId, originatorId);
        Futures.transform(edgeIdFuture, edgeId -> {
                    if (edgeId != null) {
                        try {
                            pushEventToEdge(tenantId, edgeId, edgeQueueEntityType, tbMsg, callback);
                        } catch (Exception e) {
                            log.error("Failed to push event to edge, edgeId [{}], tbMsg [{}]", edgeId, tbMsg, e);
                        }
                    }
                    return null;
                },
                MoreExecutors.directExecutor());
    }


    private ListenableFuture<EdgeId> getEdgeIdByOriginatorId(TenantId tenantId, EntityId originatorId) {
        List<EntityRelation> originatorEdgeRelations = relationService.findByToAndType(tenantId, originatorId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE);
        if (originatorEdgeRelations != null && originatorEdgeRelations.size() > 0) {
            return Futures.immediateFuture(new EdgeId(originatorEdgeRelations.get(0).getFrom().getId()));
        } else {
            return Futures.immediateFuture(null);
        }
    }


    private void pushEventToEdge(TenantId tenantId, EdgeId edgeId, EdgeQueueEntityType entityType, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        log.debug("Pushing event(s) to edge queue. tenantId [{}], edgeId [{}], entityType [{}], tbMsg [{}]", tenantId, edgeId, entityType, tbMsg);

        saveEventToEdgeQueue(tenantId, edgeId, entityType, tbMsg.getType(), tbMsg.getData(), callback);

        if (entityType.equals(EdgeQueueEntityType.RULE_CHAIN)) {
            pushRuleChainMetadataToEdge(tenantId, edgeId, tbMsg, callback);
        }
    }

    private void pushRuleChainMetadataToEdge(TenantId tenantId, EdgeId edgeId, TbMsg tbMsg, FutureCallback<Void> callback) throws IOException {
        RuleChain ruleChain = mapper.readValue(tbMsg.getData(), RuleChain.class);
        switch (tbMsg.getType()) {
            case DataConstants.ENTITY_ASSIGNED_TO_EDGE:
            case DataConstants.ENTITY_UNASSIGNED_FROM_EDGE:
            case DataConstants.ENTITY_UPDATED:
                RuleChainMetaData ruleChainMetaData = ruleChainService.loadRuleChainMetaData(tenantId, ruleChain.getId());
                saveEventToEdgeQueue(tenantId, edgeId, EdgeQueueEntityType.RULE_CHAIN_METADATA, tbMsg.getType(), mapper.writeValueAsString(ruleChainMetaData), callback);
                break;
            default:
                log.warn("Unsupported msgType [{}], tbMsg [{}]", tbMsg.getType(), tbMsg);
        }
    }


    private EdgeQueueEntityType getEdgeQueueTypeByEntityType(EntityType entityType) {
        switch (entityType) {
            case DEVICE:
                return EdgeQueueEntityType.DEVICE;
            case ASSET:
                return EdgeQueueEntityType.ASSET;
            case ENTITY_VIEW:
                return EdgeQueueEntityType.ENTITY_VIEW;
            default:
                log.info("Unsupported entity type: [{}]", entityType);
                return null;
        }
    }
}

