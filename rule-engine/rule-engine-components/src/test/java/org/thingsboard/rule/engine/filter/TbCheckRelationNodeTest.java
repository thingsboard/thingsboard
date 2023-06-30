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
package org.thingsboard.rule.engine.filter;

import com.google.common.util.concurrent.Futures;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.TestDbCallbackExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.msg.TbMsgType.POST_ATTRIBUTES_REQUEST;

class TbCheckRelationNodeTest {

    private static final TenantId TENANT_ID = new TenantId(UUID.randomUUID());
    private static final DeviceId DEVICE_ID = new DeviceId(UUID.randomUUID());
    private static final TestDbCallbackExecutor DB_EXECUTOR = new TestDbCallbackExecutor();
    private static final TbMsgMetaData EMPTY_METADATA = new TbMsgMetaData();
    private static final String EMPTY_DATA = "{}";
    private static final TbMsg EMPTY_POST_ATTRIBUTES_MSG = TbMsg.newMsg(POST_ATTRIBUTES_REQUEST.name(), DEVICE_ID, EMPTY_METADATA, EMPTY_DATA);

    private TbCheckRelationNode node;

    private TbContext ctx;
    private RelationService relationService;

    @BeforeEach
    void setUp() {
        ctx = mock(TbContext.class);
        relationService = mock(RelationService.class);

        when(ctx.getTenantId()).thenReturn(TENANT_ID);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getDbCallbackExecutor()).thenReturn(DB_EXECUTOR);

        node = new TbCheckRelationNode();
    }

    @AfterEach
    void tearDown() {
        node.destroy();
    }

    @Test
    void givenDefaultConfig_whenInit_then_throwException() {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        // WHEN
        var exception = assertThrows(TbNodeException.class, () -> node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config))));

        // THEN
        assertThat(exception.getMessage()).isEqualTo("Entity should be specified!");
    }

    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        AssetId assetId = new AssetId(UUID.randomUUID());
        config.setEntityType(assetId.getEntityType().name());
        config.setEntityId(assetId.getId().toString());

        when(relationService.checkRelationAsync(TENANT_ID, assetId, DEVICE_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(true));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntity_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        AssetId assetId = new AssetId(UUID.randomUUID());
        config.setEntityType(assetId.getEntityType().name());
        config.setEntityId(assetId.getId().toString());

        when(relationService.checkRelationAsync(TENANT_ID, assetId, DEVICE_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(false));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        AssetId assetId = new AssetId(UUID.randomUUID());
        config.setEntityType(assetId.getEntityType().name());
        config.setEntityId(assetId.getId().toString());
        config.setDirection(EntitySearchDirection.TO.name());

        when(relationService.checkRelationAsync(TENANT_ID, DEVICE_ID, assetId, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(true));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByFromAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfigWithCheckRelationToSpecificEntityAndDirectionTo_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();

        AssetId assetId = new AssetId(UUID.randomUUID());
        config.setEntityType(assetId.getEntityType().name());
        config.setEntityId(assetId.getId().toString());
        config.setDirection(EntitySearchDirection.TO.name());

        when(relationService.checkRelationAsync(TENANT_ID, DEVICE_ID, assetId, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(false));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfig_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setCheckForSingleEntity(false);
        var entityRelation = new EntityRelation();
        entityRelation.setTo(DEVICE_ID);
        entityRelation.setFrom(new AssetId(UUID.randomUUID()));
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        entityRelation.setTypeGroup(RelationTypeGroup.COMMON);

        when(relationService.findByToAndTypeAsync(TENANT_ID, DEVICE_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByFromAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfig_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setCheckForSingleEntity(false);

        when(relationService.findByToAndTypeAsync(TENANT_ID, DEVICE_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(Collections.emptyList()));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByFromAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfigDirectionTo_whenOnMsg_then_True() throws TbNodeException {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setCheckForSingleEntity(false);
        config.setDirection(EntitySearchDirection.TO.name());
        var entityRelation = new EntityRelation();
        entityRelation.setFrom(new AssetId(UUID.randomUUID()));
        entityRelation.setTo(DEVICE_ID);
        entityRelation.setType(EntityRelation.CONTAINS_TYPE);
        entityRelation.setTypeGroup(RelationTypeGroup.COMMON);

        when(relationService.findByFromAndTypeAsync(TENANT_ID, DEVICE_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(List.of(entityRelation)));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.TRUE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByToAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

    @Test
    void givenCustomConfigDirectionTo_whenOnMsg_then_False() throws TbNodeException {
        // GIVEN
        var config = new TbCheckRelationNodeConfiguration().defaultConfiguration();
        config.setCheckForSingleEntity(false);
        config.setDirection(EntitySearchDirection.TO.name());

        when(relationService.findByFromAndTypeAsync(TENANT_ID, DEVICE_ID, config.getRelationType(), RelationTypeGroup.COMMON)).thenReturn(Futures.immediateFuture(Collections.emptyList()));
        node.init(ctx, new TbNodeConfiguration(JacksonUtil.valueToTree(config)));

        // WHEN
        node.onMsg(ctx, EMPTY_POST_ATTRIBUTES_MSG);

        // THEN
        ArgumentCaptor<TbMsg> newMsgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        verify(ctx, times(1)).tellNext(newMsgCaptor.capture(), eq(TbNodeConnectionType.FALSE));
        verify(ctx, never()).tellFailure(any(), any());
        verify(relationService, never()).findByToAndTypeAsync(any(), any(), anyString(), any());
        TbMsg newMsg = newMsgCaptor.getValue();
        assertThat(newMsg).isNotNull();
        assertThat(newMsg).isSameAs(EMPTY_POST_ATTRIBUTES_MSG);
    }

}
