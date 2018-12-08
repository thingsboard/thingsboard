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
package org.thingsboard.rule.engine.transaction;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.EmptyNodeConfiguration;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgDataType;
import org.thingsboard.server.common.msg.TbMsgTransactionData;

import java.util.concurrent.ExecutionException;

import static org.thingsboard.rule.engine.api.TbRelationTypes.SUCCESS;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "transaction start",
        configClazz = EmptyNodeConfiguration.class,
        nodeDescription = "",
        nodeDetails = "",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbNodeEmptyConfig")
public class TbTransactionBeginNode implements TbNode {

    private EmptyNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, EmptyNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        log.trace("Msg enters transaction - [{}][{}]", msg.getId(), msg.getType());

        TbMsgTransactionData transactionData = new TbMsgTransactionData(msg.getId(), msg.getOriginator());
        TbMsg tbMsg = new TbMsg(msg.getId(), msg.getType(), msg.getOriginator(), msg.getMetaData(), TbMsgDataType.JSON,
                msg.getData(), transactionData, msg.getRuleChainId(), msg.getRuleNodeId(), msg.getClusterPartition());

        ctx.getRuleChainTransactionService().beginTransaction(tbMsg, startMsg -> {
                    log.trace("Transaction starting...[{}][{}]", startMsg.getId(), startMsg.getType());
                    ctx.tellNext(startMsg, SUCCESS);
                }, endMsg -> log.trace("Transaction ended successfully...[{}][{}]", endMsg.getId(), endMsg.getType()),
                throwable -> {
                    log.error("Transaction failed! [{}][{}]", tbMsg.getId(), tbMsg.getType(), throwable);
                    ctx.tellFailure(tbMsg, throwable);
                });
    }

    @Override
    public void destroy() {

    }
}
