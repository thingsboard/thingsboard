/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.Tenant;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntityRelationInfo;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.dao.alarm.AlarmOperationResult;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

@Slf4j
public abstract class AbstractNotifyEntityTest extends AbstractWebTest {

    @SpyBean
    protected TbClusterService tbClusterService;

    @SpyBean
    protected AuditLogService auditLogService;

    @Autowired
    protected AlarmService alarmService;

    protected final String msgErrorPermission = "You don't have permission to perform this operation!";
    protected final String msgErrorShouldBeSpecified = "should be specified";
    protected final String msgErrorNotFound = "Requested item wasn't found!";


    protected void testNotifyEntityAllOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                              TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                              ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllOneTimeRelation(EntityRelation relation,
                                                      TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                      ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.isNull(), Mockito.any(), Mockito.eq(EdgeEventType.RELATION),
                Mockito.eq(edgeTypeByActionType(actionType)));
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(relation.getTo());
        ArgumentMatcher<HasName> matcherEntityClassEquals = Objects::isNull;
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineNever(relation.getTo());
        matcherOriginatorId = argument -> argument.equals(relation.getFrom());
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineNever(relation.getFrom());
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllManyRelation(EntityRelation relation,
                                                   TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                   ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.isNull(), Mockito.any(), Mockito.eq(EdgeEventType.RELATION),
                Mockito.eq(edgeTypeByActionType(actionType)));
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(relation.getFrom().getClass());
        ArgumentMatcher<HasName> matcherEntityClassEquals = Objects::isNull;
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId,
                userName, actionType, cntTime * 2, 1);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, new Tenant(), cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(HasName entity, EntityId entityId, EntityId originatorId,
                                                                          TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                          ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityActionEntityEqClass(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityNeverMsgToEdgeServiceOneTime(HasName entity, EntityId entityId, TenantId tenantId,
                                                                ActionType actionType) {
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, 1);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                TenantId tenantId, CustomerId customerId, UserId userId,
                                                                String userName, ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        if (ActionType.RELATIONS_DELETED.equals(actionType)) {
            testPushMsgToRuleEngineNever(originatorId);
        } else {
            testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        }
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceNever(HasName entity, HasName originator,
                                                                     TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                     ActionType actionType, int cntTime, Object... additionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(HasName entity, HasName originator,
                                                                           TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                           ActionType actionType, ActionType actionTypeEdge,
                                                                           int cntTime, int cntTimeEdge, int cntTimeRuleEngine, Object... additionalInfo) {
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testSendNotificationMsgToEdgeServiceTimeEntityEqAny(tenantId, actionTypeEdge, cntTimeEdge);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfoClass(additionalInfo));
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTimeRuleEngine);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(HasName entity, HasName originator,
                                                                                            TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                            ActionType actionType, ActionType actionTypeEdge, int cntTime, int cntTimeEdge, int cntAdditionalInfo) {
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testSendNotificationMsgToEdgeServiceTimeEntityEqAny(tenantId, actionTypeEdge, cntTimeEdge);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                cntAdditionalInfo);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTimeEdge);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(HasName entity, HasName originator,
                                                                                      TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                      ActionType actionType, int cntTime, int cntAdditionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                cntAdditionalInfo);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                                                          TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                          ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                                               TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                               ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventMany(HasName entity, HasName originator,
                                                                       TenantId tenantId, CustomerId customerId,
                                                                       UserId userId, String userName, ActionType actionType,
                                                                       ActionType actionTypeEdge,
                                                                       int cntTime, int cntTimeEdge, int cntTimeRuleEngine,
                                                                       int cntAdditionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionTypeEdge, cntTimeEdge);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                cntAdditionalInfo);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTimeRuleEngine);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTime);
    }

    protected void testNotifyEntityMsgToEdgePushMsgToCoreOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                                                 TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                 ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        tesPushMsgToCoreTime(cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityEqualsOneTimeServiceNeverError(HasName entity, TenantId tenantId,
                                                                  UserId userId, String userName, ActionType actionType, Exception exp,
                                                                  Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_originator_NULL_UUID = createEntityId_NULL_UUID(entity);
        testNotificationMsgToEdgeServiceNeverWithActionType(entity_originator_NULL_UUID, actionType);
        ArgumentMatcher<HasName> matcherEntityEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<Exception> matcherError = argument -> argument.getMessage().contains(exp.getMessage())
                & argument.getClass().equals(exp.getClass());
        testLogEntityActionErrorAdditionalInfo(matcherEntityEquals, entity_originator_NULL_UUID, tenantId, customer_NULL_UUID, userId,
                userName, actionType, 1, matcherError, extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineNever(entity_originator_NULL_UUID);
    }

    protected void testNotifyEntityIsNullOneTimeEdgeServiceNeverError(HasName entity, TenantId tenantId,
                                                                      UserId userId, String userName, ActionType actionType, Exception exp,
                                                                      Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_originator_NULL_UUID = createEntityId_NULL_UUID(entity);
        testNotificationMsgToEdgeServiceNeverWithActionType(entity_originator_NULL_UUID, actionType);
        ArgumentMatcher<HasName> matcherEntityIsNull = Objects::isNull;
        ArgumentMatcher<Exception> matcherError = argument -> argument.getMessage().contains(exp.getMessage()) &
                argument.getClass().equals(exp.getClass());
        testLogEntityActionErrorAdditionalInfo(matcherEntityIsNull, entity_originator_NULL_UUID, tenantId, customer_NULL_UUID,
                userId, userName, actionType, 1, matcherError, extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineNever(entity_originator_NULL_UUID);
    }

    protected void testNotifyEntityNever(EntityId entityId, HasName entity) {
        entityId = entityId == null ? createEntityId_NULL_UUID(entity) : entityId;
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    private void testNotificationMsgToEdgeServiceNeverWithActionType(EntityId entityId, ActionType actionType) {
        EdgeEventActionType edgeEventActionType = ActionType.CREDENTIALS_UPDATED.equals(actionType) ?
                EdgeEventActionType.CREDENTIALS_UPDATED : edgeTypeByActionType(actionType);
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdge(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.eq(edgeEventActionType));
    }

    private void testNotificationMsgToEdgeServiceNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdge(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private void testLogEntityActionNever(EntityId entityId, HasName entity) {
        ArgumentMatcher<HasName> matcherEntity = entity == null ? Objects::isNull :
                argument -> argument.getClass().equals(entity.getClass());
        Mockito.verify(auditLogService, never()).logEntityAction(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(entityId.getClass()), Mockito.argThat(matcherEntity),
                Mockito.any(), Mockito.any());
    }

    private void testPushMsgToRuleEngineNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).pushMsgToRuleEngine(Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any());
    }

    protected void testBroadcastEntityStateChangeEventNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).broadcastEntityStateChangeEvent(Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(ComponentLifecycleEvent.class));
    }

    private void testPushMsgToRuleEngineTime(ArgumentMatcher<EntityId> matcherOriginatorId, TenantId tenantId, HasName entity, int cntTime) {
        tenantId = tenantId.isNullUid() && ((HasTenantId) entity).getTenantId() != null ? ((HasTenantId) entity).getTenantId() : tenantId;
        Mockito.verify(tbClusterService, times(cntTime)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                Mockito.argThat(matcherOriginatorId), Mockito.any(TbMsg.class), Mockito.isNull());
    }

    private void testNotificationMsgToEdgeServiceTime(EntityId entityId, TenantId tenantId, ActionType actionType, int cntTime) {
        EdgeEventActionType edgeEventActionType = ActionType.CREDENTIALS_UPDATED.equals(actionType) ?
                EdgeEventActionType.CREDENTIALS_UPDATED : edgeTypeByActionType(actionType);
        ArgumentMatcher<EntityId> matcherEntityId = cntTime == 1 ? argument -> argument.equals(entityId) :
                argument -> argument.getClass().equals(entityId.getClass());
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.any(), Mockito.argThat(matcherEntityId), Mockito.any(), Mockito.isNull(),
                Mockito.eq(edgeEventActionType));
    }

    private void testSendNotificationMsgToEdgeServiceTimeEntityEqAny(TenantId tenantId, ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.any(), Mockito.any(EntityId.class), Mockito.any(), Mockito.isNull(),
                Mockito.eq(edgeTypeByActionType(actionType)));
    }

    protected void testBroadcastEntityStateChangeEventTime(EntityId entityId, TenantId tenantId, int cntTime) {
        ArgumentMatcher<TenantId> matcherTenantIdId = cntTime > 1 || tenantId == null ? argument -> argument.getClass().equals(TenantId.class) :
                argument -> argument.equals(tenantId);
        Mockito.verify(tbClusterService, times(cntTime)).broadcastEntityStateChangeEvent(Mockito.argThat(matcherTenantIdId),
                Mockito.any(entityId.getClass()), Mockito.any(ComponentLifecycleEvent.class));
    }

    private void tesPushMsgToCoreTime(int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).pushMsgToCore(Mockito.any(ToDeviceActorNotificationMsg.class), Mockito.isNull());
    }

    private void testLogEntityAction(HasName entity, EntityId originatorId, TenantId tenantId,
                                     CustomerId customerId, UserId userId, String userName,
                                     ActionType actionType, int cntTime, Object... additionalInfo) {
        ArgumentMatcher<HasName> matcherEntityEquals = entity == null ? Objects::isNull : argument -> argument.toString().equals(entity.toString());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName,
                actionType, cntTime, extractMatcherAdditionalInfo(additionalInfo));
    }

    private void testLogEntityActionEntityEqClass(HasName entity, EntityId originatorId, TenantId tenantId,
                                                  CustomerId customerId, UserId userId, String userName,
                                                  ActionType actionType, int cntTime, Object... additionalInfo) {
        ArgumentMatcher<HasName> matcherEntityEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName,
                actionType, cntTime, extractMatcherAdditionalInfo(additionalInfo));
    }

    private void testLogEntityActionAdditionalInfo(ArgumentMatcher<HasName> matcherEntity, ArgumentMatcher<EntityId> matcherOriginatorId,
                                                   TenantId tenantId, ArgumentMatcher<CustomerId> matcherCustomerId,
                                                   ArgumentMatcher<UserId> matcherUserId, String userName, ActionType actionType,
                                                   int cntTime, List<ArgumentMatcher<Object>> matcherAdditionalInfos) {
        switch (matcherAdditionalInfos.size()) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.argThat(matcherAdditionalInfos.get(0)));
                break;
            case 2:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.argThat(matcherAdditionalInfos.get(0)),
                                Mockito.argThat(matcherAdditionalInfos.get(1)));
                break;
            case 3:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.argThat(matcherAdditionalInfos.get(0)),
                                Mockito.argThat(matcherAdditionalInfos.get(1)),
                                Mockito.argThat(matcherAdditionalInfos.get(2)));
                break;
            default:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull());
        }
    }

    private void testLogEntityActionAdditionalInfoAny(ArgumentMatcher<HasName> matcherEntity, ArgumentMatcher<EntityId> matcherOriginatorId,
                                                      TenantId tenantId, ArgumentMatcher<CustomerId> matcherCustomerId,
                                                      ArgumentMatcher<UserId> matcherUserId, String userName,
                                                      ActionType actionType, int cntTime, int cntAdditionalInfo) {
        switch (cntAdditionalInfo) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.any());
                break;
            case 2:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.any(),
                                Mockito.any());
                break;
            case 3:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.any(),
                                Mockito.any(),
                                Mockito.any());
                break;
            default:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.argThat(matcherCustomerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull());
        }
    }

    private void testLogEntityActionErrorAdditionalInfo(ArgumentMatcher<HasName> matcherEntity, EntityId originatorId, TenantId tenantId,
                                                        CustomerId customerId, UserId userId, String userName, ActionType actionType,
                                                        int cntTime, ArgumentMatcher<Exception> matcherError,
                                                        List<ArgumentMatcher<Object>> matcherAdditionalInfos) {
        ArgumentMatcher<UserId> matcherUserId = userId == null ? argument -> argument.getClass().equals(UserId.class) :
                argument -> argument.equals(userId);
        switch (matcherAdditionalInfos.size()) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.eq(originatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.argThat(matcherError),
                                Mockito.argThat(matcherAdditionalInfos.get(0)));
                break;
            case 2:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.eq(originatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.argThat(matcherError),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(0))),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(1))));
            case 3:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.eq(originatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.argThat(matcherError),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(0))),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(1))),
                                Mockito.argThat(Mockito.eq(matcherAdditionalInfos.get(2))));
                break;
            default:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.argThat(matcherUserId),
                                Mockito.eq(userName),
                                Mockito.eq(originatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.argThat(matcherError));
        }
    }

    private List<ArgumentMatcher<Object>> extractMatcherAdditionalInfo(Object... additionalInfos) {
        List<ArgumentMatcher<Object>> matcherAdditionalInfos = new ArrayList<>(additionalInfos.length);
        for (Object additionalInfo : additionalInfos) {
            matcherAdditionalInfos.add(argument -> argument.equals(extractParameter(additionalInfo.getClass(), additionalInfo)));
        }
        return matcherAdditionalInfos;
    }

    private List<ArgumentMatcher<Object>> extractMatcherAdditionalInfoClass(Object... additionalInfos) {
        List<ArgumentMatcher<Object>> matcherAdditionalInfos = new ArrayList<>(additionalInfos.length);
        for (Object additionalInfo : additionalInfos) {
            matcherAdditionalInfos.add(argument -> argument.getClass().equals(extractParameter(additionalInfo.getClass(), additionalInfo).getClass()));
        }
        return matcherAdditionalInfos;
    }

    private <T> T extractParameter(Class<T> clazz, Object additionalInfo) {
        T result = null;
        if (additionalInfo != null) {
            Object paramObject = additionalInfo;
            if (clazz.isInstance(paramObject)) {
                result = clazz.cast(paramObject);
            }
        }
        return result;
    }

    protected EntityId createEntityId_NULL_UUID(HasName entity) {
        return EntityIdFactory.getByTypeAndUuid(entityClassToEntityTypeName(entity), ModelConstants.NULL_UUID);
    }

    protected String msgErrorFieldLength(String fieldName) {
        return "length of " + fieldName + " must be equal or less than 255";
    }

    protected String msgErrorNoFound(String entityClassName, String assetIdStr) {
        return entityClassName + " with id [" + assetIdStr + "] is not found";
    }

    private String entityClassToEntityTypeName(HasName entity) {
        String entityType = entityClassToString(entity);
        return "SAVE_OTA_PACKAGE_INFO_REQUEST".equals(entityType) || "OTA_PACKAGE_INFO".equals(entityType) ?
                EntityType.OTA_PACKAGE.name().toUpperCase(Locale.ENGLISH) : entityType;
    }

    private String entityClassToString(HasName entity) {
        String className = entity.getClass().toString()
                .substring(entity.getClass().toString().lastIndexOf(".") + 1);
        List str = className.chars()
                .mapToObj(x -> (Character.isUpperCase(x)) ? "_" + Character.toString(x) : Character.toString(x))
                .collect(Collectors.toList());
        return String.join("", str).toUpperCase(Locale.ENGLISH).substring(1);
    }


    public <T, E> void testDeleteEntity_ExistsRelationToEntity_Error_RestoreRelationToEntity_DeleteRelation_DeleteEntity_Ok(
            TenantId tenantId, CustomerId customerId, Class<T> clazzTestEntity, EntityId testEntityId, T savedTestEntity,
            EntityId entityIdFrom, E entityFromWithoutEntityTo, String urlGetTestEntity, String urlDeleteTestEntity,
            String urlUpdateEntityFrom, String entityTestNameClass, String name, String entityTestMsgNotDelete, int cntOtherEntity
    ) throws Exception {

        Map<EntityId, HasName> entities = createEntities(entityTestNameClass + " " + name, cntOtherEntity);

        Alarm savedAlarmForTestEntity = createAlarm(tenantId, customerId, "Alarm by " + name, entityTestNameClass, testEntityId);
        // Create Alarms for other entity
        entities.forEach((k, v) -> createAlarm(tenantId, customerId, "Alarm by " + v.getName() + " " + name, entityTestNameClass, k));

        entities.put(testEntityId, (HasName)savedTestEntity);

        // Create entityRelations: from -> entityFrom, to -> entities
        String typeRelation = EntityRelation.CONTAINS_TYPE;
        Map<EntityRelation, String> entityRelations = createEntityRelations(entityIdFrom, entities, typeRelation);
        Optional<Map.Entry<EntityRelation, String>> relationMapTestEntityTo = entityRelations.entrySet().stream().filter(e -> e.getKey().getTo().equals(testEntityId)).findFirst();
        assertTrue("TestEntityRelation is found after " + entityTestNameClass + " deletion 'success'!", relationMapTestEntityTo.isPresent());
        String urlRelationTestEntityTo = relationMapTestEntityTo.get().getValue();

        String testEntityIdStr = testEntityId.getId().toString();
        doDelete(urlDeleteTestEntity + testEntityIdStr)
                .andExpect(status().isBadRequest())
                .andExpect(statusReason(containsString(entityTestMsgNotDelete)));

        savedTestEntity = doGet(urlGetTestEntity + testEntityIdStr, clazzTestEntity);
        assertNotNull(entityTestNameClass + " is not found!", savedTestEntity);
        assertEquals(entityTestNameClass + " after delete error is not equals origin!", savedTestEntity, savedTestEntity);


        String urlEntityFroms = String.format("/api/relations?fromId=%s&fromType=%s",
                entityIdFrom.getId(), EntityType.DEVICE_PROFILE);
        List<EntityRelationInfo> relationsInfos =
                JacksonUtil.convertValue(doGet(urlEntityFroms, JsonNode.class), new TypeReference<>() {
                });
        int numOfRelations = entityRelations.size();
        assertNotNull("Relations is not found!", relationsInfos);
        assertEquals("List of found relations is not equal to number of created relations!",
                numOfRelations, relationsInfos.size());
        EntityId expectTestEntityId = testEntityId;
        Optional<EntityRelationInfo> expectTestEntityRelationInfo = relationsInfos.stream().filter(k -> k.getTo().equals(expectTestEntityId)).findFirst();
        assertTrue("TestEntityRelation is not found after " + entityTestNameClass + " deletion 'bad request'!", expectTestEntityRelationInfo.isPresent());
        String expectTestEntityRelationToIdStr = expectTestEntityRelationInfo.get().getTo().getId().toString();

        AlarmOperationResult afterErrorDeleteTestEntityAlarmOperationResult = alarmService.createOrUpdateAlarm(savedAlarmForTestEntity);
        assertTrue("AfterErrorDelete" + entityTestNameClass + "AlarmOperationResult is not success!", afterErrorDeleteTestEntityAlarmOperationResult.isSuccessful());
        assertTrue("List of propagatedEntities is not equal to number of created propagatedEntities!",
                afterErrorDeleteTestEntityAlarmOperationResult.getPropagatedEntitiesList().size() > 0);
        assertTrue(entityTestNameClass + "Id in propagatedEntities is not equal saved" + entityTestNameClass + "Id!",
                afterErrorDeleteTestEntityAlarmOperationResult.getPropagatedEntitiesList()
                .stream().filter(p -> p.equals(testEntityId))
                .findFirst()
                .isPresent());

        doPost(urlUpdateEntityFrom, entityFromWithoutEntityTo)
                .andExpect(status().isOk());

        doDelete(urlDeleteTestEntity + testEntityIdStr)
                .andExpect(status().isOk());
        doGet(urlGetTestEntity + testEntityIdStr)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(entityTestNameClass, testEntityIdStr))));

        doGet(urlRelationTestEntityTo)
                .andExpect(status().isNotFound())
                .andExpect(statusReason(containsString(msgErrorNoFound(entityTestNameClass, expectTestEntityRelationToIdStr))));
        relationsInfos =
                JacksonUtil.convertValue(doGet(urlEntityFroms, JsonNode.class), new TypeReference<>() { });
        assertNotNull("Relations is not found!", relationsInfos);
        assertEquals("List of found relations is not equal to number of relations left!",
                numOfRelations - 1, relationsInfos.size());
        expectTestEntityRelationInfo = relationsInfos.stream().filter(k -> k.getTo().equals(expectTestEntityId)).findFirst();
        assertTrue("TestEntityRelation is found after " + entityTestNameClass + " deletion 'success'!", expectTestEntityRelationInfo.isEmpty());

        AlarmOperationResult afterSuccessDeleteTestEntityAlarmOperationResult = alarmService.createOrUpdateAlarm(savedAlarmForTestEntity);
        assertTrue("AfterSuccessDelete" + entityTestNameClass + "AlarmOperationResult is not success!", afterSuccessDeleteTestEntityAlarmOperationResult.isSuccessful());
        assertTrue(entityTestNameClass + "Id in propagatedEntities is equal saved" + entityTestNameClass + "Id!",
                afterSuccessDeleteTestEntityAlarmOperationResult.getPropagatedEntitiesList()
                        .stream().filter(p -> p.equals(testEntityId))
                        .findFirst()
                        .isEmpty());
    }

    private Alarm createAlarm(TenantId tenantId, CustomerId customerId, String name, String entityNameClass, EntityId entityId) {
        Alarm alarm = Alarm.builder()
                .tenantId(tenantId)
                .customerId(customerId)
                .originator(entityId)
                .status(AlarmStatus.ACTIVE_UNACK)
                .severity(AlarmSeverity.CRITICAL)
                .type(name)
                .propagate(true)
                .build();
        AlarmOperationResult alarmOperationResult = alarmService.createOrUpdateAlarm(alarm);
        assertTrue("AlarmOperationResult is not success!", alarmOperationResult.isSuccessful());
        assertTrue("List of propagatedEntities is not equal to number of created propagatedEntities!",
                alarmOperationResult.getPropagatedEntitiesList().size() > 0);
        assertTrue(entityNameClass + "Id in propagatedEntities is not equal saved" + entityNameClass + "Id!",
                alarmOperationResult.getPropagatedEntitiesList()
                        .stream().filter(p -> p.equals(entityId))
                        .findFirst()
                        .isPresent());
        Alarm savedAlarm = alarmOperationResult.getAlarm();
        assertNotNull("SavedAlarm is not found!", savedAlarm);

        return alarm;
    }

    private EntityRelation createEntityRelation(EntityId entityIdFrom, EntityId entityIdTo, String url, String typeRelation) throws Exception {
        EntityRelation relation = new EntityRelation(entityIdFrom, entityIdTo, typeRelation);
        doPost("/api/relation", relation).andExpect(status().isOk());

        EntityRelation foundRelation = doGet(url, EntityRelation.class);
        Assert.assertNotNull("Relation is not found!", foundRelation);
        assertEquals("Found relation is not equals origin!", relation, foundRelation);

        return foundRelation;
    }

    private Map<EntityId, HasName> createEntities(String name, int cntOtherEntity) throws Exception {
        ListeningExecutorService executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
        List<ListenableFuture<Device>> futures = new ArrayList<>(cntOtherEntity);
        for (int i = 0; i < cntOtherEntity; i++) {
            Device device = new Device();
            device.setName(name + i);
            device.setType("default");
            futures.add(executor.submit(() ->
                    doPost("/api/device", device, Device.class)));
        }
        List<Device> devices = Futures.allAsList(futures).get(TIMEOUT, TimeUnit.SECONDS);
        Map<EntityId, Device> deviceMap = Maps.uniqueIndex(devices, Device::getId)
                .entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue()));

        return deviceMap.entrySet().stream()
                .collect(Collectors.toMap(
                        e -> e.getKey(),
                        e -> e.getValue()));
    }

    private Map<EntityRelation, String> createEntityRelations(EntityId entityIdFrom, Map<EntityId, HasName> entityTos, String typeRelation) {
        Map<EntityRelation, String> entityRelations = new HashMap<>();
        entityTos.keySet().forEach(k -> {
            try {
                String url = String.format("/api/relation?fromId=%s&fromType=%s&relationType=%s&toId=%s&toType=%s",
                        entityIdFrom.getId(), entityIdFrom.getEntityType(),
                        typeRelation,
                        k.getId(), k.getEntityType()
                );
                EntityRelation relation = createEntityRelation(entityIdFrom, k, url, typeRelation);
                entityRelations.put(relation, url);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return entityRelations;
    }
}
