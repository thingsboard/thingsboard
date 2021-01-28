/**
 * Copyright © 2016-2021 The Thingsboard Authors
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

import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.RuleEngineException;
import org.thingsboard.server.common.msg.queue.TbMsgCallback;

import java.util.List;

import static org.thingsboard.common.util.DonAsynchron.withCallback;
import static org.thingsboard.rule.engine.api.TbRelationTypes.FAILURE;

/**
 * Created by ashvayka on 19.01.18.
 */
@Slf4j
public abstract class TbAbstractTransformNode implements TbNode {

    private TbTransformNodeConfiguration config;

    @Override
    public void init(TbContext context, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbTransformNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        withCallback(transform(ctx, msg),
                m -> transformSuccess(ctx, msg, m),
                t -> transformFailure(ctx, msg, t),
                ctx.getDbCallbackExecutor());
    }

    protected void transformFailure(TbContext ctx, TbMsg msg, Throwable t) {
        ctx.tellFailure(msg, t);
    }

    protected void transformSuccess(TbContext ctx, TbMsg msg, TbMsg m) {
        if (m != null) {
            ctx.tellSuccess(m);
        } else {
            ctx.tellNext(msg, FAILURE);
        }
    }

    protected void transformSuccess(TbContext ctx, TbMsg msg, List<TbMsg> msgs) {
        if (msgs != null && !msgs.isEmpty()) {
            if (msgs.size() == 1) {
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
                msgs.forEach(newMsg -> ctx.enqueueForTellNext(newMsg, "Success", wrapper::onSuccess, wrapper::onFailure));
            }
        } else {
            ctx.tellNext(msg, FAILURE);
        }
    }

    protected abstract ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg);

    public void setConfig(TbTransformNodeConfiguration config) {
        this.config = config;
    }
}
