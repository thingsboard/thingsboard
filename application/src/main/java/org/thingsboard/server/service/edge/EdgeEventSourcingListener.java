/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
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

import javax.annotation.PostConstruct;

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
@Component
@RequiredArgsConstructor
@Slf4j
public class EdgeEventSourcingListener {

    private final TbClusterService tbClusterService;
    private final EdgeSynchronizationManager edgeSynchronizationManager;

    @PostConstruct
    public void init() {
        log.info("EdgeEventSourcingListener initiated");
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(SaveEntityEvent<?> event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        try {
            if (!isValidEdgeEventEntity(event)) {
                return;
            }
            log.trace("[{}] SaveEntityEvent called: {}", event.getTenantId(), event);
            EdgeEventActionType action = Boolean.TRUE.equals(event.getAdded()) ? EdgeEventActionType.ADDED : EdgeEventActionType.UPDATED;
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), null, event.getEntityId(),
                    null, null, action);
        } catch (Exception e) {
            log.error("[{}] failed to process SaveEntityEvent: {}", event.getTenantId(), event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(DeleteEntityEvent<?> event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        EntityType entityType = event.getEntityId().getEntityType();
        try {
            if (EntityType.EDGE.equals(entityType) || EntityType.TENANT.equals(entityType) || EntityType.TB_RESOURCE.equals(entityType)) {
                return;
            }
            log.trace("[{}] DeleteEntityEvent called: {}", event.getTenantId(), event);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), event.getEdgeId(), event.getEntityId(),
                    JacksonUtil.toString(event.getEntity()), null, EdgeEventActionType.DELETED);
        } catch (Exception e) {
            log.error("[{}] failed to process DeleteEntityEvent: {}", event.getTenantId(), event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(ActionEntityEvent<?> event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        if (EntityType.DEVICE.equals(event.getEntityId().getEntityType())
                && ActionType.ASSIGNED_TO_TENANT.equals(event.getActionType())) {
            return;
        }
        try {
            log.trace("[{}] ActionEntityEvent called: {}", event.getTenantId(), event);
            tbClusterService.sendNotificationMsgToEdge(event.getTenantId(), event.getEdgeId(), event.getEntityId(),
                    event.getBody(), null, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()));
        } catch (Exception e) {
            log.error("[{}] failed to process ActionEntityEvent: {}", event.getTenantId(), event);
        }
    }

    @TransactionalEventListener(fallbackExecution = true)
    public void handleEvent(RelationActionEvent event) {
        if (edgeSynchronizationManager.isSync()) {
            return;
        }
        try {
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
                    JacksonUtil.toString(relation), EdgeEventType.RELATION, EdgeUtils.getEdgeEventActionTypeByActionType(event.getActionType()));
        } catch (Exception e) {
            log.error("[{}] failed to process RelationActionEvent: {}", event.getTenantId(), event);
        }
    }

    private boolean isValidEdgeEventEntity(SaveEntityEvent<?> event) {
        switch (event.getEntityId().getEntityType()) {
            case RULE_CHAIN:
                RuleChain ruleChain = (RuleChain) event.getEntity();
                return RuleChainType.EDGE.equals(ruleChain.getType());
            case USER:
                User user = (User) event.getEntity();
                return !Authority.SYS_ADMIN.equals(user.getAuthority());
            case OTA_PACKAGE:
                if (event.getEntity() instanceof OtaPackageInfo) {
                    OtaPackageInfo otaPackageInfo = (OtaPackageInfo) event.getEntity();
                    return otaPackageInfo.hasUrl() || otaPackageInfo.isHasData();
                }
                break;
            case ALARM:
                if (event.getEntity() instanceof AlarmApiCallResult) {
                    AlarmApiCallResult alarmApiCallResult = (AlarmApiCallResult) event.getEntity();
                    return alarmApiCallResult.isModified();
                }
                break;
            case TENANT:
                return !event.getAdded();
            case API_USAGE_STATE:
            case TB_RESOURCE:
            case EDGE:
                return false;
        }
        // Default: If the entity doesn't match any of the conditions, consider it as valid.
        return true;
    }
}
