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

import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Locale;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.thingsboard.server.service.entitiy.DefaultTbNotificationEntityService.edgeTypeByActionType;

public abstract class AbstractNotifyEntityTest extends AbstractWebTest {

    @SpyBean
    protected TbClusterService tbClusterService;

    @SpyBean
    protected AuditLogService auditLogService;

    protected void testNotifyEntityOk(HasName entity, EntityId entityId, EntityId originatorId,
                                      TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                      ActionType actionType) {
        testSendNotificationMsgToEdgeServiceOk(entityId, tenantId, actionType);
        testLogEntityActionOk(entity, originatorId, tenantId, customerId, userId, userName, actionType);
        testPushMsgToRuleEngineOk(entity, originatorId, tenantId, customerId, userId, userName, actionType);
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityDeleteOk(HasName entity, EntityId entityId, EntityId originatorId,
                                            TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                            ActionType actionType) {
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdgeService(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.any());
        testLogEntityActionOk(entity, originatorId, tenantId, customerId, userId, userName, actionType);
        testPushMsgToRuleEngineOk(entity, originatorId, tenantId, customerId, userId, userName, actionType);
    }

    private void testNotifyEntityError(EntityId entityId, HasName entity, TenantId tenantId,
                                       UserId userId, String userName, ActionType actionType, Exception exp,
                                       Object... additionalInfo) {
        CustomerId customer_NULL_UUID = (CustomerId) EntityIdFactory.getByTypeAndUuid(EntityType.CUSTOMER, ModelConstants.NULL_UUID);
        EntityId entity_NULL_UUID = EntityIdFactory.getByTypeAndUuid(EntityType.valueOf(entity.getClass().toString().substring(entity.getClass().toString().lastIndexOf(".") + 1).toUpperCase(Locale.ENGLISH)),
                ModelConstants.NULL_UUID);
        testNotificationMsgToEdgeServiceNever(entityId);
        if (additionalInfo.length > 0) {
            Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId),
                    Mockito.eq(customer_NULL_UUID), Mockito.eq(userId), Mockito.eq(userName),
                    Mockito.eq(entity_NULL_UUID), Mockito.any(entity.getClass()), Mockito.eq(actionType),
                    Mockito.any(exp.getClass()), Mockito.eq(additionalInfo));
        } else {
            Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId),
                    Mockito.eq(customer_NULL_UUID), Mockito.eq(userId), Mockito.eq(userName),
                    Mockito.eq(entity_NULL_UUID), Mockito.any(entity.getClass()), Mockito.eq(actionType),
                    Mockito.any(exp.getClass()), Mockito.isNull());
        }
        Mockito.verify(tbClusterService, never()).pushMsgToRuleEngine(Mockito.any(), Mockito.any(entityId.getClass()),
                Mockito.any(), Mockito.any());
        Mockito.reset(tbClusterService, auditLogService);
    }

    protected void testNotifyEntityNever(EntityId entityId, HasName entity) {
        testNotificationMsgToEdgeServiceNever(entityId);
        testLogEntityActionNever(entityId, entity);
        testPushMsgToRuleEngineNever(entityId);
    }

    protected void testNotificationMsgToEdgeServiceNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).sendNotificationMsgToEdgeService(Mockito.any(),
                Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any(), Mockito.any());
    }

    protected void testLogEntityActionNever(EntityId entityId, HasName entity) {
        Mockito.verify(auditLogService, never()).logEntityAction(Mockito.any(), Mockito.any(),
                Mockito.any(), Mockito.any(), Mockito.any(entityId.getClass()), Mockito.any(entity.getClass()),
                Mockito.any(), Mockito.any());
    }

    protected void testPushMsgToRuleEngineNever(EntityId entityId) {
        Mockito.verify(tbClusterService, never()).pushMsgToRuleEngine(Mockito.any(),
                Mockito.any(entityId.getClass()), Mockito.any(), Mockito.any());
    }

    private void testLogEntityActionOk(HasName entity, EntityId originatorId,
                                       TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                       ActionType actionType) {
        Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                Mockito.eq(userId), Mockito.eq(userName), Mockito.eq(originatorId),
                Mockito.eq(entity), Mockito.eq(actionType), Mockito.isNull());
    }

    private void testPushMsgToRuleEngineOk(HasName entity, EntityId originatorId,
                                           TenantId tenantId, CustomerId customerId, UserId userId, String userName,
                                           ActionType actionType) {

        Mockito.verify(auditLogService, times(1)).logEntityAction(Mockito.eq(tenantId), Mockito.eq(customerId),
                Mockito.eq(userId), Mockito.eq(userName), Mockito.eq(originatorId),
                Mockito.eq(entity), Mockito.eq(actionType), Mockito.isNull());
    }

    private void testSendNotificationMsgToEdgeServiceOk(EntityId entityId, TenantId tenantId, ActionType actionType) {
        Mockito.verify(tbClusterService, times(1)).sendNotificationMsgToEdgeService(Mockito.eq(tenantId),
                Mockito.isNull(), Mockito.eq(entityId), Mockito.isNull(), Mockito.isNull(),
                Mockito.eq(edgeTypeByActionType(actionType)));
    }

}
