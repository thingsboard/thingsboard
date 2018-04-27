/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.transform;

import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.Futures;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.dao.asset.AssetService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TbChangeOriginatorNodeTest {

    private TbChangeOriginatorNode node;

    @Mock
    private TbContext ctx;
    @Mock
    private AssetService assetService;


    @Test
    public void originatorCanBeChangedToCustomerId() throws TbNodeException {
        init(false);
        AssetId assetId = new AssetId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);

        RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "ASSET", assetId, new TbMsgMetaData(), "{}", ruleChainId, ruleNodeId, 0L);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(assetId)).thenReturn(Futures.immediateFuture(asset));

        node.onMsg(ctx, msg);

        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).newMsg(typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(customerId, originatorCaptor.getValue());
    }

    @Test
    public void newChainCanBeStarted() throws TbNodeException {
        init(true);
        AssetId assetId = new AssetId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);

        RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "ASSET", assetId, new TbMsgMetaData(), "{}", ruleChainId, ruleNodeId, 0L);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(assetId)).thenReturn(Futures.immediateFuture(asset));

        node.onMsg(ctx, msg);
        ArgumentCaptor<String> typeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<EntityId> originatorCaptor = ArgumentCaptor.forClass(EntityId.class);
        ArgumentCaptor<TbMsgMetaData> metadataCaptor = ArgumentCaptor.forClass(TbMsgMetaData.class);
        ArgumentCaptor<String> dataCaptor = ArgumentCaptor.forClass(String.class);
        verify(ctx).newMsg(typeCaptor.capture(), originatorCaptor.capture(), metadataCaptor.capture(), dataCaptor.capture());

        assertEquals(customerId, originatorCaptor.getValue());
    }

    @Test
    public void exceptionThrownIfCannotFindNewOriginator() throws TbNodeException {
        init(true);
        AssetId assetId = new AssetId(UUIDs.timeBased());
        CustomerId customerId = new CustomerId(UUIDs.timeBased());
        Asset asset = new Asset();
        asset.setCustomerId(customerId);

        RuleChainId ruleChainId = new RuleChainId(UUIDs.timeBased());
        RuleNodeId ruleNodeId = new RuleNodeId(UUIDs.timeBased());

        TbMsg msg = new TbMsg(UUIDs.timeBased(), "ASSET", assetId, new TbMsgMetaData(), "{}", ruleChainId, ruleNodeId, 0L);

        when(ctx.getAssetService()).thenReturn(assetService);
        when(assetService.findAssetByIdAsync(assetId)).thenReturn(Futures.immediateFailedFuture(new IllegalStateException("wrong")));

        node.onMsg(ctx, msg);
        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(ctx).tellError(same(msg), captor.capture());
        Throwable value = captor.getValue();
        assertEquals("wrong", value.getMessage());
    }

    public void init(boolean startNewChain) throws TbNodeException {
        TbChangeOriginatorNodeConfiguration config = new TbChangeOriginatorNodeConfiguration();
        config.setOriginatorSource(TbChangeOriginatorNode.CUSTOMER_SOURCE);
        config.setStartNewChain(startNewChain);
        ObjectMapper mapper = new ObjectMapper();
        TbNodeConfiguration nodeConfiguration = new TbNodeConfiguration(mapper.valueToTree(config));

        node = new TbChangeOriginatorNode();
        node.init(null, nodeConfiguration);
    }
}