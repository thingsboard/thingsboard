/**
 * Copyright © 2016-2020 The Thingsboard Authors
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

import com.google.common.base.Function;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.util.EntitiesAlarmOriginatorIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesCustomerIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesTenantIdAsyncLoader;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.HashSet;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "转换发起者",
        configClazz = TbChangeOriginatorNodeConfiguration.class,
        nodeDescription = "将消息发起着更改为租户/客户/相关实体",
        nodeDetails = "使用配置的关系方向和关系类型找到相关实体。" +
                "如果找到多个相关联的实体，则只有第一个实体被当做新的发起者，其他实体将被丢弃。",
        uiResources = {"static/rulenode/rulenode-core-config.js", "static/rulenode/rulenode-core-config.css"},
        configDirective = "tbTransformationNodeChangeOriginatorConfig",
        icon = "find_replace"
)
public class TbChangeOriginatorNode extends TbAbstractTransformNode {

    protected static final String CUSTOMER_SOURCE = "CUSTOMER";
    protected static final String TENANT_SOURCE = "TENANT";
    protected static final String RELATED_SOURCE = "RELATED";
    protected static final String ALARM_ORIGINATOR_SOURCE = "ALARM_ORIGINATOR";

    private TbChangeOriginatorNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbChangeOriginatorNodeConfiguration.class);
        validateConfig(config);
        setConfig(config);
    }

    @Override
    protected ListenableFuture<TbMsg> transform(TbContext ctx, TbMsg msg) {
        ListenableFuture<? extends EntityId> newOriginator = getNewOriginator(ctx, msg.getOriginator());
        return Futures.transform(newOriginator, (Function<EntityId, TbMsg>) n -> {
            if (n == null || n.isNullUid()) {
                return null;
            }
            return ctx.transformMsg(msg, msg.getType(), n, msg.getMetaData(), msg.getData());
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<? extends EntityId> getNewOriginator(TbContext ctx, EntityId original) {
        switch (config.getOriginatorSource()) {
            case CUSTOMER_SOURCE:
                return EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctx, original);
            case TENANT_SOURCE:
                return EntitiesTenantIdAsyncLoader.findEntityIdAsync(ctx, original);
            case RELATED_SOURCE:
                return EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, original, config.getRelationsQuery());
            case ALARM_ORIGINATOR_SOURCE:
                return EntitiesAlarmOriginatorIdAsyncLoader.findEntityIdAsync(ctx, original);
            default:
                return Futures.immediateFailedFuture(new IllegalStateException("Unexpected originator source " + config.getOriginatorSource()));
        }
    }

    private void validateConfig(TbChangeOriginatorNodeConfiguration conf) {
        HashSet<String> knownSources = Sets.newHashSet(CUSTOMER_SOURCE, TENANT_SOURCE, RELATED_SOURCE, ALARM_ORIGINATOR_SOURCE);
        if (!knownSources.contains(conf.getOriginatorSource())) {
            log.error("Unsupported source [{}] for TbChangeOriginatorNode", conf.getOriginatorSource());
            throw new IllegalArgumentException("Unsupported source TbChangeOriginatorNode" + conf.getOriginatorSource());
        }

        if (conf.getOriginatorSource().equals(RELATED_SOURCE)) {
            if (conf.getRelationsQuery() == null) {
                log.error("Related source for TbChangeOriginatorNode should have relations query. Actual [{}]",
                        conf.getRelationsQuery());
                throw new IllegalArgumentException("Wrong config for RElated Source in TbChangeOriginatorNode" + conf.getOriginatorSource());
            }
        }

    }

    @Override
    public void destroy() {

    }
}
