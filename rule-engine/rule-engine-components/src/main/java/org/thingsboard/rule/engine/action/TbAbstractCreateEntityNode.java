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
package org.thingsboard.rule.engine.action;

import org.springframework.util.StringUtils;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;

public abstract class TbAbstractCreateEntityNode<C extends TbAbstractCreateEntityNodeConfiguration> implements TbNode {

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = initConfiguration(configuration);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        processOnMsg(ctx, msg);
    }

    @Override
    public void destroy() {
    }

    protected abstract C initConfiguration(TbNodeConfiguration configuration) throws TbNodeException;

    protected abstract void processOnMsg(TbContext ctx, TbMsg msg) throws TbNodeException;

    protected void validatePatternSubstitution(String pattern, String substitution) {
        if (StringUtils.isEmpty(substitution) || substitution.trim().length() == 0) {
            throw new IllegalArgumentException("Message parameter for " + pattern + " pattern has invalid value!");
        }
    }

}
