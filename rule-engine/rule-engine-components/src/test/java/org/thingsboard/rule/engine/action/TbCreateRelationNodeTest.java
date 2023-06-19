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
package org.thingsboard.rule.engine.action;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ListeningExecutor;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeConnectionType;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.DataConstants;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.EntitySearchDirection;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.relation.RelationService;

import java.util.Collections;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.thingsboard.server.common.data.msg.TbMsgType.ENTITY_CREATED;

@RunWith(MockitoJUnitRunner.class)
public class TbCreateRelationNodeTest {

    private static final String RELATION_TYPE_CONTAINS = "Contains";

    private TbCreateRelationNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private AssetService assetService;
    @Mock
    private RelationService relationService;

    private TbMsg msg;

    private RuleChainId ruleChainId = new RuleChainId(Uuids.timeBased());
    private RuleNodeId ruleNodeId = new RuleNodeId(Uuids.timeBased());

    private ListeningExecutor dbExecutor;

    @Before
    public void before() {
        dbExecutor = new ListeningExecutor() {
            @Override
            public <T> ListenableFuture<T> executeAsync(Callable<T> task) {
                try {
                    return Futures.immediateFuture(task.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void execute(Runnable command) {
                command.run();
            }
        };
    }

    @Test
    public void testCreateNewRelation() throws TbNodeException {
        init(createRelationNodeConfig());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(ENTITY_CREATED.name(), deviceId, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(RELATION_TYPE_CONTAINS), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, RELATION_TYPE_CONTAINS, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    @Test
    public void testDeleteCurrentRelationsCreateNewRelation() throws TbNodeException {
        init(createRelationNodeConfigWithRemoveCurrentRelations());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(ENTITY_CREATED.name(), deviceId, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        EntityRelation relation = new EntityRelation();
        when(ctx.getRelationService().findByToAndTypeAsync(any(), eq(msg.getOriginator()), eq(RELATION_TYPE_CONTAINS), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(Collections.singletonList(relation)));
        when(ctx.getRelationService().deleteRelationAsync(any(), eq(relation))).thenReturn(Futures.immediateFuture(true));
        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(RELATION_TYPE_CONTAINS), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, RELATION_TYPE_CONTAINS, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        verify(ctx).tellNext(msg, TbNodeConnectionType.SUCCESS);
    }

    @Test
    public void testCreateNewRelationAndChangeOriginator() throws TbNodeException {
        init(createRelationNodeConfigWithChangeOriginator());

        DeviceId deviceId = new DeviceId(Uuids.timeBased());

        AssetId assetId = new AssetId(Uuids.timeBased());
        Asset asset = new Asset();
        asset.setId(assetId);

        when(assetService.findAssetByTenantIdAndName(any(), eq("AssetName"))).thenReturn(asset);
        when(assetService.findAssetByIdAsync(any(), eq(assetId))).thenReturn(Futures.immediateFuture(asset));

        TbMsgMetaData metaData = new TbMsgMetaData();
        metaData.putValue("name", "AssetName");
        metaData.putValue("type", "AssetType");
        msg = TbMsg.newMsg(ENTITY_CREATED.name(), deviceId, metaData, TbMsgDataType.JSON, "{}", ruleChainId, ruleNodeId);

        when(ctx.getRelationService().checkRelationAsync(any(), eq(assetId), eq(deviceId), eq(RELATION_TYPE_CONTAINS), eq(RelationTypeGroup.COMMON)))
                .thenReturn(Futures.immediateFuture(false));
        when(ctx.getRelationService().saveRelationAsync(any(), eq(new EntityRelation(assetId, deviceId, RELATION_TYPE_CONTAINS, RelationTypeGroup.COMMON))))
                .thenReturn(Futures.immediateFuture(true));

        node.onMsg(ctx, msg);
        ArgumentCaptor<TbMsg> msgCaptor = ArgumentCaptor.forClass(TbMsg.class);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).transformMsg(msgCaptor.capture(), typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(assetId, originatorCaptor.getValue());
    }

    public void init(TbCreateRelationNodeConfiguration configuration) throws TbNodeException {
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(configuration));

        when(ctx.getDbCallbackExecutor()).thenReturn(dbExecutor);
        when(ctx.getRelationService()).thenReturn(relationService);
        when(ctx.getAssetService()).thenReturn(assetService);

        node = new TbCreateRelationNode();
        node.init(ctx, nodeConfiguration);
    }

    private TbCreateRelationNodeConfiguration createRelationNodeConfig() {
        TbCreateRelationNodeConfiguration configuration = new TbCreateRelationNodeConfiguration();
        configuration.setDirection(EntitySearchDirection.FROM.name());
        configuration.setRelationType(RELATION_TYPE_CONTAINS);
        configuration.setEntityCacheExpiration(300);
        configuration.setEntityType("ASSET");
        configuration.setEntityNamePattern("${name}");
        configuration.setEntityTypePattern("${type}");
        configuration.setCreateEntityIfNotExists(false);
        configuration.setChangeOriginatorToRelatedEntity(false);
        configuration.setRemoveCurrentRelations(false);
        return configuration;
    }

    private TbCreateRelationNodeConfiguration createRelationNodeConfigWithRemoveCurrentRelations() {
        TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setRemoveCurrentRelations(true);
        return configuration;
    }

    private TbCreateRelationNodeConfiguration createRelationNodeConfigWithChangeOriginator() {
        TbCreateRelationNodeConfiguration configuration = createRelationNodeConfig();
        configuration.setChangeOriginatorToRelatedEntity(true);
        return configuration;
    }
}
