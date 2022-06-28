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

import lombok.extern.slf4j.Slf4j;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleEvent;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.ToDeviceActorNotificationMsg;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.service.gateway_device.GatewayNotificationsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

@Slf4j
public abstract class AbstractNotifyEntityTest extends AbstractWebTest {

    @SpyBean
    protected TbClusterService tbClusterService;

    @SpyBean
    protected AuditLogService auditLogService;

    @SpyBean
    protected GatewayNotificationsService gatewayNotificationsService;

    protected void testNotifyEntityAllOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                              TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                              ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testSendNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityAllOneTimeLogEntityActionEntityEqClass(HasName entity, EntityId entityId, EntityId originatorId,
                                                                          TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                          ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testSendNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityActionEntityEqClass(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityNeverMsgToEdgeServiceOneTime(HasName entity, EntityId entityId, TenantId tenantId,
                                                                ActionType actionType) {
        testSendNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, 1);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                TenantId tenantId, CustomerId customerId, UserId userId,
                                                                String userName, ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceNever(HasName entity, HasName originator,
                                                                     TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                     ActionType actionType, int cntTime, Object... additionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceNever(entityId);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, customerId, userId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, cntTime);
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
        testLogEntityActionAdditionalInfo(matcherEntityClassEquals, matcherOriginatorId, tenantId, customerId, userId, userName, actionType, cntTime,
                extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, cntTimeRuleEngine);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceEntityEqAnyAdditionalInfoAny(HasName entity, HasName originator,
                                                                                            TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                            ActionType actionType, ActionType actionTypeEdge, int cntTime, int cntTimeEdge, int cntAdditionalInfo) {
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testSendNotificationMsgToEdgeServiceTimeEntityEqAny(tenantId, actionTypeEdge, cntTimeEdge);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, customerId, userId, userName, actionType, cntTime,
                cntAdditionalInfo);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, cntTimeEdge);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyManyEntityManyTimeMsgToEdgeServiceNeverAdditionalInfoAny(HasName entity, HasName originator,
                                                                                      TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                      ActionType actionType, int cntTime, int cntAdditionalInfo) {
        EntityId entityId = createEntityId_NULL_UUID(entity);
        EntityId originatorId = createEntityId_NULL_UUID(originator);
        testNotificationMsgToEdgeServiceNever(entityId);
        ArgumentMatcher<HasName> matcherEntityClassEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.getClass().equals(originatorId.getClass());
        testLogEntityActionAdditionalInfoAny(matcherEntityClassEquals, matcherOriginatorId, tenantId, customerId, userId, userName, actionType, cntTime,
                cntAdditionalInfo);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityBroadcastEntityStateChangeEventOneTimeMsgToEdgeServiceNever(HasName entity, EntityId entityId, EntityId originatorId,
                                                                                               TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                                               ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testPushMsgToRuleEngineTime(matcherOriginatorId, tenantId, cntTime);
        testBroadcastEntityStateChangeEventTime(entityId, tenantId, cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }


    protected void testNotifyEntityMsgToEdgePushMsgToCoreOneTime(HasName entity, EntityId entityId, EntityId originatorId,
                                                                 TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                                 ActionType actionType, Object... additionalInfo) {
        int cntTime = 1;
        testSendNotificationMsgToEdgeServiceTime(entityId, tenantId, actionType, cntTime);
        testLogEntityAction(entity, originatorId, tenantId, customerId, userId, userName, actionType, cntTime, additionalInfo);
        tesPushMsgToCoreTime(cntTime);
        Mockito.reset(tbClusterService, auditLogService);
    }


    protected void testNotifyEntityEqualsOneTimeError(HasName entity, TenantId tenantId,
                                                      UserId userId, String userName, ActionType actionType, Exception exp,
                                                      Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_originator_NULL_UUID = createEntityId_NULL_UUID(entity);
        testNotificationMsgToEdgeServiceNever(entity_originator_NULL_UUID);
        ArgumentMatcher<HasName> matcherEntityEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<Exception> matcherError = argument -> argument.getMessage().contains(exp.getMessage())
                & argument.getClass().equals(exp.getClass());
        testLogEntityActionErrorAdditionalInfo(matcherEntityEquals, entity_originator_NULL_UUID, tenantId, customer_NULL_UUID, userId,
                userName, actionType, 1, matcherError, extractMatcherAdditionalInfo(additionalInfo));
        testPushMsgToRuleEngineNever(entity_originator_NULL_UUID);
    }

    protected void testNotifyEntityIsNullOneTimeError(HasName entity, TenantId tenantId,
                                                      UserId userId, String userName, ActionType actionType, Exception exp,
                                                      Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_originator_NULL_UUID = createEntityId_NULL_UUID(entity);
        testNotificationMsgToEdgeServiceNever(entity_originator_NULL_UUID);
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

    protected void testNotificationUpdateGatewayOneTime(Device device, Device oldDevice) {
        Mockito.verify(gatewayNotificationsService, times(1)).onDeviceUpdated(Mockito.eq(device), Mockito.eq(oldDevice));
    }

    protected void testNotificationUpdateGatewayTime(int cntTimes) {
        Mockito.verify(gatewayNotificationsService, times(cntTimes)).onDeviceUpdated(Mockito.any(Device.class), Mockito.isNull());
    }

    protected void testNotificationUpdateGatewayNever() {
        Mockito.verify(gatewayNotificationsService, never()).onDeviceUpdated(Mockito.any(Device.class), Mockito.any(Device.class));
    }

    protected void testNotificationDeleteGatewayOneTime(Device device) {
        Mockito.verify(gatewayNotificationsService, times(1)).onDeviceDeleted(device);
    }

    protected void testNotificationDeleteGatewayNever() {
        Mockito.verify(gatewayNotificationsService, never()).onDeviceDeleted(Mockito.any(Device.class));
    }

    private void testNotificationMsgToEdgeServiceNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdge(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.any());
    }

    private void testLogEntityActionNever(EntityId entityId, HasName entity) {
        Mockito.verify(auditLogService, never()).logEntityAction(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(entity.getClass()),
                Mockito.any(), Mockito.any());
    }

    private void testPushMsgToRuleEngineNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).pushMsgToRuleEngine(Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any());
    }

    private void testPushMsgToRuleEngineTime(ArgumentMatcher<EntityId> matcherOriginatorId, TenantId tenantId, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).pushMsgToRuleEngine(Mockito.eq(tenantId),
                Mockito.argThat(matcherOriginatorId), Mockito.any(TbMsg.class), Mockito.isNull());
    }

//    private void testPushMsgToRuleEngineOne(EntityId originatorId, TenantId tenantId) {
//        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId) ;
//        Mockito.verify(tbClusterService, times(1)).pushMsgToRuleEngine(Mockito.eq(tenantId),
//                Mockito.argThat(matcherOriginatorId), Mockito.any(TbMsg.class), Mockito.isNull());
//    }

    private void testSendNotificationMsgToEdgeServiceTime(EntityId entityId, TenantId tenantId, ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.any(), Mockito.eq(entityId), Mockito.any(), Mockito.isNull(),
                Mockito.eq(edgeTypeByActionType(actionType)));
    }

    private void testSendNotificationMsgToEdgeServiceTimeEntityEqAny(TenantId tenantId, ActionType actionType, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).sendNotificationMsgToEdge(Mockito.eq(tenantId),
                Mockito.any(), Mockito.any(EntityId.class), Mockito.any(), Mockito.isNull(),
                Mockito.eq(edgeTypeByActionType(actionType)));
    }

    private void testBroadcastEntityStateChangeEventTime(EntityId entityId, TenantId tenantId, int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).broadcastEntityStateChangeEvent(Mockito.eq(tenantId),
                Mockito.any(entityId.getClass()), Mockito.any(ComponentLifecycleEvent.class));
    }

    private void tesPushMsgToCoreTime(int cntTime) {
        Mockito.verify(tbClusterService, times(cntTime)).pushMsgToCore(Mockito.any(ToDeviceActorNotificationMsg.class), Mockito.isNull());
    }

    private void testLogEntityAction(HasName entity, EntityId originatorId, TenantId tenantId,
                                     CustomerId customerId, UserId userId, String userName,
                                     ActionType actionType, int cntTime, Object... additionalInfo) {
        ArgumentMatcher<HasName> matcherEntityEquals = argument -> argument.equals(entity);
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testLogEntityActionAdditionalInfo(matcherEntityEquals, matcherOriginatorId, tenantId, customerId, userId, userName,
                actionType, cntTime, extractMatcherAdditionalInfo(additionalInfo));
    }

    private void testLogEntityActionEntityEqClass(HasName entity, EntityId originatorId, TenantId tenantId,
                                                  CustomerId customerId, UserId userId, String userName,
                                                  ActionType actionType, int cntTime, Object... additionalInfo) {
        ArgumentMatcher<HasName> matcherEntityEquals = argument -> argument.getClass().equals(entity.getClass());
        ArgumentMatcher<EntityId> matcherOriginatorId = argument -> argument.equals(originatorId);
        testLogEntityActionAdditionalInfo(matcherEntityEquals, matcherOriginatorId, tenantId, customerId, userId, userName,
                actionType, cntTime, extractMatcherAdditionalInfo(additionalInfo));
    }

    private void testLogEntityActionAdditionalInfo(ArgumentMatcher<HasName> matcherEntity, ArgumentMatcher<EntityId> matcherOriginatorId,
                                                   TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                   ActionType actionType, int cntTime, List<ArgumentMatcher<Object>> matcherAdditionalInfos) {
        switch (matcherAdditionalInfos.size()) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
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
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
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
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
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
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull());
        }
    }

    private void testLogEntityActionAdditionalInfoAny(ArgumentMatcher<HasName> matcherEntity, ArgumentMatcher<EntityId> matcherOriginatorId,
                                                      TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                                      ActionType actionType, int cntTime, int cntAdditionalInfo) {
        switch (cntAdditionalInfo) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.anyString());
                break;
            case 2:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.anyString(),
                                Mockito.anyString());
                break;
            case 3:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
                                Mockito.eq(userName),
                                Mockito.argThat(matcherOriginatorId),
                                Mockito.argThat(matcherEntity),
                                Mockito.eq(actionType),
                                Mockito.isNull(),
                                Mockito.anyString(),
                                Mockito.anyString(),
                                Mockito.anyString());
                break;
            default:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
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
        switch (matcherAdditionalInfos.size()) {
            case 1:
                Mockito.verify(auditLogService, times(cntTime))
                        .logEntityAction(Mockito.eq(tenantId),
                                Mockito.eq(customerId),
                                Mockito.eq(userId),
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
                                Mockito.eq(userId),
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
                                Mockito.eq(userId),
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
                                Mockito.eq(userId),
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

    private EntityId createEntityId_NULL_UUID(HasName entity) {
        return EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(entity.getClass().toString()
                        .substring(entity.getClass().toString().lastIndexOf(".") + 1).toUpperCase(Locale.ENGLISH)),
                ModelConstants.NULL_UUID);
    }
}
