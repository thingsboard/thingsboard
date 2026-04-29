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
package org.thingsboard.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.Tenant;
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
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@Slf4j
public abstract class AbstractNotifyEntityTest extends AbstractWebTest {

    @SpyBean
    protected TbClusterService tbClusterService;

    @SpyBean
    protected AuditLogService auditLogService;

    @MockBean
    BuildProperties buildProperties;

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

    protected void testNotifyAssignUnassignEntityAllOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                                            TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                            ActionType actionType, ActionType actionTypeEdge, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionTypeEdge, cntTime);
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
                Mockito.eq(EdgeUtils.getEdgeEventActionTypeByActionType(actionType)), Mockito.any());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(relation.getTo());
        ArgumentMatcher<HasName> matcherEntityClassEquals = Objects::isNull;
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        matcherOriginatorId = argument -> argument.equals(relation.getFrom());
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllManyRelation(EntityRelation relation,
                                                   TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                   ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.isNull(), Mockito.any(), Mockito.eq(EdgeEventType.RELATION),
                Mockito.eq(EdgeUtils.getEdgeEventActionTypeByActionType(actionType)), Mockito.any());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(relation.getFrom().getClass());
        ArgumentMatcher<HasName> matcherEntityClassEquals = Objects::isNull;
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId,
                userName, actionType, cntTime * 2, 1);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, new Tenant(), cntTime * 3);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(HasName entity, EntityId entityId, EntityId originatorId,
                                                                          TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                          ActionType actionType, ActionType actionTypeEdge, Object... additionalInfo) {
        testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(tenantId, entity, entityId, originatorId, tenantId, customerId, userId, userName, actionType, actionTypeEdge, additionalInfo);
    }

    protected void testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(TenantId entityTenantId, HasName entity, EntityId entityId, EntityId originatorId,
                                                                          TenantId authTenantId, CustomerId customerId, UserId userId, String userName,
                                                                          ActionType actionType, ActionType actionTypeEdge, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, entityTenantId, actionTypeEdge, cntTime);
        testLogEntityActionEntityEqClass(entity, originatorId, authTenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, authTenantId, entity, cntTime);
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
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(HasName entity, HasName originator,
                                                                           TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                           ActionType actionType,
                                                                           int cntTime, int cntTimeEdge, int cntTimeRuleEngine, Object... additionalInfo) {
        testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(tenantId, entity, originator, tenantId, customerId, userId, userName, actionType,
                cntTime, cntTimeEdge, cntTimeRuleEngine, additionalInfo);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAny(TenantId entityTenantId, HasName entity, HasName originator,
                                                                           TenantId authTenantId, CustomerId customerId, UserId userId, String userName,
                                                                           ActionType actionType,
                                                                           int cntTime, int cntTimeEdge, int cntTimeRuleEngine, Object... additionalInfo) {
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testSendNotificationMsgToEdgeServiceTimeEntityEqAny(entityTenantId, actionType, cntTimeEdge);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, authTenantId, matcherCustomerId, matcherUserId, userName, actionType, cntTime,
                extractMatcherAdditionalInfoClass(additionalInfo));
        testPushMsgToRuleEngineTime(matcherOriginatorId, authTenantId, entity, cntTimeRuleEngine);

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
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ? argument -> true : actualCustomerId -> actualCustomerId.equals(customerId);
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

    protected void testNotifyEntityBroadcastEntityStateChangeEventManyTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                                                TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                                ActionType actionType, int cntTimeBroadcast, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTimeBroadcast);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEdgeStateChangeEventManyTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                               TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                               ActionType actionType, int cntTimeBroadcast, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceNeverWithActionType(entityId, actionType);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, entity, cntTime);
        testBroadcastEdgeStateChangeEventTime(tenantId, cntTimeBroadcast);
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
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTime * 2);
    }

    protected void testNotifyEntityMsgToEdgePushMsgToCoreOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                                                 TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                 ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        testPushMsgToCoreTime(cntTime);
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
                EdgeEventActionType.CREDENTIALS_UPDATED : EdgeUtils.getEdgeEventActionTypeByActionType(actionType);
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdge(Mockito.any(), Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.eq(edgeEventActionType), Mockito.any());
    }

    private void testNotificationMsgToEdgeServiceNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdge(Mockito.any(), Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
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
                EdgeEventActionType.CREDENTIALS_UPDATED : EdgeUtils.getEdgeEventActionTypeByActionType(actionType);
        ArgumentMatcher<EntityId> matcherEntityId = cntTime == 1 ? argument -> argument.equals(entityId) :
                argument -> argument.getClass().equals(entityId.getClass());
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.any(), Mockito.argThat(matcherEntityId), Mockito.any(), Mockito.isNull(),
                Mockito.eq(edgeEventActionType), Mockito.any());
    }

    private void testSendNotificationMsgToEdgeServiceTimeEntityEqAny(TenantId tenantId, ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.any(), Mockito.any(EntityId.class), Mockito.any(), Mockito.isNull(),
                Mockito.eq(EdgeUtils.getEdgeEventActionTypeByActionType(actionType)), Mockito.any());
    }

    protected void testBroadcastEntityStateChangeEventTime(EntityId entityId, TenantId tenantId, int cntTime) {
        ArgumentMatcher<TenantId> matcherTenantIdId = cntTime > 1 || tenantId == null ? argument -> argument.getClass().equals(TenantId.class) :
                argument -> argument.equals(tenantId);
        Mockito.verify(tbClusterService, times(cntTime)).broadcastEntityStateChangeEvent(Mockito.argThat(matcherTenantIdId),
                Mockito.any(entityId.getClass()), Mockito.any(ComponentLifecycleEvent.class));
    }

    protected void testBroadcastEdgeStateChangeEventTime(TenantId tenantId, int cntTime) {
        ArgumentMatcher<TenantId> matcherTenantIdId = cntTime > 1 || tenantId == null ? argument -> argument.getClass().equals(TenantId.class) :
                argument -> argument.equals(tenantId);
        Mockito.verify(tbClusterService, times(cntTime)).onEdgeStateChangeEvent(Mockito.any(ComponentLifecycleMsg.class));
    }

    private void testPushMsgToCoreTime(int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).pushMsgToCore(Mockito.any(ToDeviceActorNotificationMsg.class), Mockito.isNull());
    }

    protected void testLogEntityAction(HasName entity, EntityId originatorId, TenantId tenantId,
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

    protected void testLogEntityActionEntityEqClass(HasName entity, EntityId originatorId, TenantId tenantId,
                                                    CustomerId customerId, UserId userId, String userName,
                                                    ActionType actionType, int cntTime, Object... additionalInfo) {
        ArgumentMatcher<HasName> matcherEntityEquals = argument -> entity.getClass().isAssignableFrom(argument.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        ArgumentMatcher<CustomerId> matcherCustomerId = customerId == null ?
                argument -> argument.getClass().equals(CustomerId.class) : argument -> argument.equals(customerId);
        ArgumentMatcher<UserId> matcherUserId = userId == null ?
                argument -> argument.getClass().equals(UserId.class) : argument -> argument.equals(userId);
        testLogEntityActionAdditionalInfo(matcherEntityEquals, matcherOriginatorId, tenantId, matcherCustomerId, matcherUserId, userName,
                actionType, cntTime, extractMatcherAdditionalInfo(additionalInfo));
    }

    protected void testLogEntityActionError(EntityId originatorId, TenantId tenantId,
                                            CustomerId customerId, UserId userId, String userName,
                                            ActionType actionType, Exception exception, Object... additionalInfo) {
        ArgumentMatcher<Exception> matcherError = argument -> argument.getMessage().contains(exception.getMessage())
                & argument.getClass().equals(exception.getClass());
        testLogEntityActionErrorAdditionalInfo(Objects::isNull, originatorId, tenantId, customerId, userId, userName,
                actionType, 1, matcherError, extractMatcherAdditionalInfo(additionalInfo));
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
                                Mockito.argThat(matcherAdditionalInfos.get(0)),
                                Mockito.argThat(matcherAdditionalInfos.get(1)));
                break;
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
                                Mockito.argThat(matcherAdditionalInfos.get(0)),
                                Mockito.argThat(matcherAdditionalInfos.get(1)),
                                Mockito.argThat(matcherAdditionalInfos.get(2)));
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
        return fieldName + " length must be equal or less than 255";
    }

    protected static String msgErrorNoFound(String entityClassName, String entityIdStr) {
        return entityClassName + " with id [" + entityIdStr + "] is not found";
    }

    private String entityClassToEntityTypeName(HasName entity) {
        String entityType = entityClassToString(entity);
        return switch (entityType) {
            case "SAVE_OTA_PACKAGE_INFO_REQUEST", "OTA_PACKAGE_INFO" -> "OTA_PACKAGE";
            case "TB_RESOURCE_INFO" -> "TB_RESOURCE";
            default -> entityType;
        };
    }

    private String entityClassToString(HasName entity) {
        String className = entity.getClass().toString()
                .substring(entity.getClass().toString().lastIndexOf(".") + 1);
        List<String> str = className.chars()
                .mapToObj(x -> (Character.isUpperCase(x)) ? "_" + Character.toString(x) : Character.toString(x))
                .collect(Collectors.toList());
        return String.join("", str).toUpperCase(Locale.ENGLISH).substring(1);
    }
}
