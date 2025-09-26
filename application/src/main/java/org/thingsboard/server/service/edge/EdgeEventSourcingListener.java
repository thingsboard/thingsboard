/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.service.edge;

import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmComment;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.domain.Domain;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.common.data.rule.RuleChainType;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.eventsourcing.ActionEntityEvent;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.RelationActionEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.tenant.TenantService;

/**
 * This event listener does not support async event processing because relay on ThreadLocal
 * Another possible approach is to implement a special annotation and a bunch of classes similar to TransactionalApplicationListener
 * This class is the simplest approach to maintain edge synchronization within the single class.
 * <p>
 * For async event publishers, you have to decide whether publish event on creating async task in the same thread where dao method called
 * @Autowired
 * EdgeEventSynchronizationManager edgeSynchronizationManager
 * ...
 *   //some async write action make future
 *   if (!edgeSynchronizationManager.isSync()) {
 *     future.addCallback(eventPublisher.publishEvent(...))
 *   }
 * */
@Slf4j
@Component
@RequiredArgsConstructor
public class EdgeEventSourcingListener {

    private final TbClusterService tbClusterService;

    private final TenantService tenantService;
    private final EdgeSynchronizationManager edgeSynchronizationManager;

    @PostConstruct
    public void init() {
        log.debug("EdgeEventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent<?> event) {
        if (Boolean.FALSE.equals(event.getBroadcastEvent())) {
            log.trace("Ignoring event {}", event);
            return;
        }

        try {
            if (!isValidSaveEntityEventForEdgeProcessing(event)) {
                return;
            }
            log.trace("[{}] SaveEntityEvent called: {}", event.getTenantId(), event);
            boolean isCreated = Boolean.TRUE.equals(event.getCreated());
            String body = getBodyMsgForEntityEvent(event.getEntity());
            EdgeEventType type = getEdgeEventTypeForEntityEvent(event.getEntity());
            EdgeEventActionType action = getActionForEntityEvent(event.getEntity(), isCreated);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, event.getEntityId(),
                    body, type, action, edgeSynchronizationManager.getEdgeId().get());
        } catch (Exception e) {
            log.error("[{}] failed to process SaveEntityEvent: {}", event.getTenantId(), event, e);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        TenantId tenantId = event.getTenantId();
        EntityType entityType = event.getEntityId().getEntityType();
        if (!tenantId.isSysTenantId() && !tenantService.tenantExists(tenantId)) {
            log.debug("[{}] Ignoring DeleteEntityEvent because tenant does not exist: {}", tenantId, event);
            return;
        }
        try {
            if (EntityType.TENANT == entityType || EntityType.EDGE == entityType) {
                return;
            }
            log.trace("[{}] DeleteEntityEvent called: {}", tenantId, event);
            EdgeEventType type = getEdgeEventTypeForEntityEvent(event.getEntity());
            EdgeEventActionType actionType = getEdgeEventActionTypeForEntityEvent(event.getEntity());
            tbClusterService.sendNotificationMsgToEdge(tenantId, null, event.getEntityId(),
                    JacksonUtil.toString(event.getEntity()), type, actionType,
                    edgeSynchronizationManager.getEdgeId().get());
        } catch (Exception e) {
            log.error("[{}] failed to process DeleteEntityEvent: {}", tenantId, event, e);
        }
    }

    private EdgeEventActionType getEdgeEventActionTypeForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return EdgeEventActionType.DELETED_COMMENT;
        } else if (entity instanceof Alarm) {
            return EdgeEventActionType.ALARM_DELETE;
        }
        return EdgeEventActionType.DELETED;
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent<?> event) {
        if (EntityType.DEVICE.equals(event.getEntityId().getEntityType()) && ActionType.ASSIGNED_TO_TENANT.equals(event.getActionType())) {
            return;
        }
        if (EntityType.ALARM.equals(event.getEntityId().getEntityType())) {
            return;
        }
        try {
            if (event.getEntityId().getEntityType().equals(EntityType.RULE_CHAIN) && event.getEdgeId() != null && event.getActionType().equals(ActionType.ASSIGNED_TO_EDGE)) {
                try {
                    Edge edge = JacksonUtil.fromString(event.getBody(), Edge.class);
                    if (edge != null && new RuleChainId(event.getEntityId().getId()).equals(edge.getRootRuleChainId())) {
                        log.trace("[{}] skipping ASSIGNED_TO_EDGE event of RULE_CHAIN entity in case Edge Root Rule Chain: {}", event.getTenantId(), event);
                        return;
                    }
                } catch (Exception ignored) {
                    return;
                }
            }
            log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), event.getEdgeId(), event.getEntityId(),
                    event.getBody(), null, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()),
                    edgeSynchronizationManager.getEdgeId().get());
        } catch (Exception e) {
            log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event, e);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(RelationActionEvent event) {
        try {
            TenantId tenantId = event.getTenantId();
            if (ActionType.RELATION_DELETED.equals(event.getActionType()) && !tenantService.tenantExists(tenantId)) {
                log.debug("[{}] Ignoring RelationActionEvent because tenant does not exist: {}", tenantId, event);
                return;
            }
            EntityRelation relation = event.getRelation();
            if (relation == null) {
                log.trace("[{}] skipping RelationActionEvent event in case relation is null: {}", event.getTenantId(), event);
                return;
            }
            if (!RelationTypeGroup.COMMON.equals(relation.getTypeGroup())) {
                log.trace("[{}] skipping RelationActionEvent event in case NOT COMMON relation type group: {}", event.getTenantId(), event);
                return;
            }
            log.trace("[{}] RelationActionEvent called: {}", event.getTenantId(), event);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, null,
                    JacksonUtil.toString(relation), EdgeEventType.RELATION, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()),
                    edgeSynchronizationManager.getEdgeId().get());
        } catch (Exception e) {
            log.error("[{}] failed to process RelationActionEvent: {}", event.getTenantId(), event, e);
        }
    }

    private boolean isValidSaveEntityEventForEdgeProcessing(SaveEntityEvent<?> event) {
        Object entity = event.getEntity();
        Object oldEntity = event.getOldEntity();
        if (event.getEntityId() != null) {
            switch (event.getEntityId().getEntityType()) {
                case RULE_CHAIN:
                    if (entity instanceof RuleChain ruleChain) {
                        return RuleChainType.EDGE.equals(ruleChain.getType());
                    }
                    break;
                case USER:
                    if (entity instanceof User user) {
                        if (Authority.SYS_ADMIN.equals(user.getAuthority())) {
                            return false;
                        }
                        if (oldEntity != null) {
                            user = JacksonUtil.clone(user);
                            User oldUser = JacksonUtil.clone((User) oldEntity);
                            cleanUpUserAdditionalInfo(oldUser);
                            cleanUpUserAdditionalInfo(user);
                            return !user.equals(oldUser);
                        }
                    }
                    break;
                case OTA_PACKAGE:
                    if (entity instanceof OtaPackageInfo otaPackageInfo) {
                        return otaPackageInfo.hasUrl() || otaPackageInfo.isHasData();
                    }
                    break;
                case ALARM:
                    if (entity instanceof AlarmApiCallResult || entity instanceof Alarm || entity instanceof EntityAlarm) {
                        return false;
                    }
                    break;
                case TENANT:
                    return !event.getCreated();
                case API_USAGE_STATE, EDGE:
                    return false;
                case DOMAIN:
                    if (entity instanceof Domain domain) {
                        return domain.isPropagateToEdge();
                    }
            }
        }
        // Default: If the entity doesn't match any of the conditions, consider it as valid.
        return true;
    }

    private void cleanUpUserAdditionalInfo(User user) {
        if (user.getAdditionalInfo() instanceof NullNode) {
            user.setAdditionalInfo(null);
        }
        if (user.getAdditionalInfo() instanceof ObjectNode additionalInfo) {
            if (additionalInfo.isEmpty()) {
                user.setAdditionalInfo(null);
            } else {
                user.setAdditionalInfo(additionalInfo);
            }
        }
        user.setVersion(null);
    }

    private EdgeEventType getEdgeEventTypeForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return EdgeEventType.ALARM_COMMENT;
        }
        return null;
    }

    private String getBodyMsgForEntityEvent(Object entity) {
        if (entity instanceof AlarmComment) {
            return JacksonUtil.toString(entity);
        } else if (entity instanceof CalculatedField calculatedField) {
            return JacksonUtil.toString(calculatedField.getEntityId());
        }
        return null;
    }

    private EdgeEventActionType getActionForEntityEvent(Object entity, boolean isCreated) {
        if (entity instanceof AlarmComment) {
            return isCreated ? EdgeEventActionType.ADDED_COMMENT : EdgeEventActionType.UPDATED_COMMENT;
        }
        return isCreated ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED;
    }

}
