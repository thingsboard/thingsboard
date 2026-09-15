// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.transform;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.msg.TbNodeConnectionType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
public abstract class TbAbstractTransformNode<C> implements TbNode {

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        config = loadNodeConfiguration(ctx, configuration);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(transform(ctx, msg),
                m -> transformSuccess(ctx, msg, m),
                t -> transformFailure(ctx, msg, t),
                MoreExecutors.directExecutor());
    }

    protected abstract C loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    protected void transformFailure(TbContext ctx, TbMsg msg, Throwable t) {
        ctx.tellFailure(msg, t);
    }

    protected void transformSuccess(TbContext ctx, TbMsg msg, List<TbMsg> msgs) {
        if (msgs == null || msgs.isEmpty()) {
            ctx.tellFailure(msg, new RuntimeException("Message or messages list are empty!"));
        } else if (msgs.size() == 1) {
            ctx.tellSuccess(msgs.get(0));
        } else {
            TbMsgCallbackWrapper wrapper = new MultipleTbMsgsCallbackWrapper(msgs.size(), new TbMsgCallback() {
                @Override
                public void onSuccess() {
                    ctx.ack(msg);
                }

                @Override
                public void onFailure(RuleEngineException e) {
                    ctx.tellFailure(msg, e);
                }
            });
            msgs.forEach(newMsg -> ctx.enqueueForTellNext(newMsg, TbNodeConnectionType.SUCCESS, wrapper::onSuccess, wrapper::onFailure));
        }
    }

    protected abstract ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg);

}
