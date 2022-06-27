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
package org.thingsboard.server.service.action;

import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueProducer;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.service.profile.TbDeviceProfileCache;
import org.thingsboard.server.service.queue.DefaultTbClusterService;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
public class EntityActionServiceTest {

    public static final String CUSTOMER_ID = "customerId";
    public static final String DEFAULT_QUEUE_NAME = "Main";

    EntityActionService actionService;

    @Mock
    AuditLogService auditLogService;
    @Mock
    TbQueueProducerProvider producerProvider;
    @Mock
    TbDeviceProfileCache deviceProfileCache;
    @Mock
    TbQueueProducer<TbProtoQueueMsg<TransportProtos.ToRuleEngineMsg>> queueProducer;
    @Spy
    @InjectMocks
    DefaultTbClusterService clusterService;

    TenantId tenantId = new TenantId(UUID.randomUUID());
    CustomerId customerId = new CustomerId(UUID.randomUUID());

    DeviceProfile deviceProfile;

    @Before
    public void init() {
        actionService = new EntityActionService(clusterService, auditLogService);
        deviceProfile = new DeviceProfile();
        deviceProfile.setDefaultQueueName(DEFAULT_QUEUE_NAME);
        deviceProfile.setTenantId(tenantId);
        deviceProfile.setDefaultRuleChainId(new RuleChainId(UUID.randomUUID()));

        when(deviceProfileCache.get(any(TenantId.class), any(DeviceId.class))).thenReturn(deviceProfile);
        when(producerProvider.getRuleEngineMsgProducer()).thenReturn(queueProducer);
        doNothing().when(queueProducer).send(any(), any(), any());
        doNothing().when(clusterService).pushMsgToRuleEngine(any(TenantId.class), any(), any(), any());
    }

    @Test
    public void testPushEntityActionToRuleEngine_whenActionTypeEqualsRelationUpdated_thenSendTwoEvent() {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new DeviceId(UUID.randomUUID()));
        relation.setTo(new AssetId(UUID.randomUUID()));

        pushEventAndVerify(relation, ActionType.RELATION_ADD_OR_UPDATE);
    }

    @Test
    public void testPushEntityActionToRuleEngine_whenActionTypeEqualsRelationDeleted_thenSendTwoEvent() {
        EntityRelation relation = new EntityRelation();
        relation.setFrom(new DeviceId(UUID.randomUUID()));
        relation.setTo(new AssetId(UUID.randomUUID()));

        pushEventAndVerify(relation, ActionType.RELATION_DELETED);
    }

    void pushEventAndVerify(EntityRelation relation, ActionType actionType) {
        actionService.pushEntityActionToRuleEngine(relation.getFrom(),
                null,
                tenantId,
                customerId,
                actionType,
                null,
                relation);

        actionService.pushEntityActionToRuleEngine(relation.getTo(),
                null,
                tenantId,
                customerId,
                actionType,
                null,
                relation);

        verifyMetadataTbMsg(relation, actionType);
    }

    private void verifyMetadataTbMsg(EntityRelation relation, ActionType actionType) {
        String msgType = getMsgType(actionType);
        String data = JacksonUtil.toString(JacksonUtil.valueToTree(relation));

        TbMsg tbMsgFrom = getTbMsg(EntitySearchDirection.FROM, msgType, relation.getFrom(), data);
        TbMsg tbMsgTo = getTbMsg(EntitySearchDirection.TO, msgType, relation.getTo(), data);

        ArgumentCaptor<TbMsg> argumentCaptorTbMsg = ArgumentCaptor.forClass(TbMsg.class);
        verify(clusterService, times(2)).pushMsgToRuleEngine(any(), any(), argumentCaptorTbMsg.capture(), any());
        List<TbMsg> allValuesTbMsg = argumentCaptorTbMsg.getAllValues();
        checkTbMsgMetadata(tbMsgFrom, tbMsgTo, allValuesTbMsg);
    }

    private void checkTbMsgMetadata(TbMsg tbMsgFrom, TbMsg tbMsgTo, List<TbMsg> allValuesTbMsg) {
        boolean hasFromMetadata = false;
        boolean hasToMetadata = false;
        for (TbMsg tbMsg : allValuesTbMsg) {
            if (tbMsg.getMetaData().equals(tbMsgFrom.getMetaData())) {
                hasFromMetadata = true;
            } else if (tbMsg.getMetaData().equals(tbMsgTo.getMetaData())) {
                hasToMetadata = true;
            }
        }
        Assert.assertTrue(hasFromMetadata);
        Assert.assertTrue(hasToMetadata);
    }

    @NotNull
    private TbMsg getTbMsg(EntitySearchDirection direction, String msgType, EntityId entityId, String data) {
        TbMsgMetaData metaDataFrom = new TbMsgMetaData();
        metaDataFrom.putValue(DataConstants.RELATION_DIRECTION_MSG_ORIGINATOR, direction.name());
        metaDataFrom.putValue(CUSTOMER_ID, customerId.getId().toString());
        return TbMsg.newMsg(msgType, entityId, customerId, metaDataFrom, TbMsgDataType.JSON, data);
    }

    @NotNull
    private String getMsgType(ActionType actionType) {
        if (actionType == ActionType.RELATION_ADD_OR_UPDATE) {
            return DataConstants.RELATION_ADD_OR_UPDATE;
        } else if (actionType == ActionType.RELATION_DELETED) {
            return DataConstants.RELATION_DELETED;
        }
        return "";
    }

}