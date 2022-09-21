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
package org.thingsboard.rule.engine.transform;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.util.EntitiesAlarmOriginatorIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesByNameAndTypeLoader;
import org.thingsboard.rule.engine.util.EntitiesCustomerIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.rule.engine.util.EntitiesTenantIdAsyncLoader;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "change originator",
        configClazz = TbChangeOriginatorNodeConfiguration.class,
        nodeDescription = "Change Message Originator To Tenant/Customer/Related Entity/Alarm Originator",
        nodeDetails = "Related Entity found using configured relation direction and Relation Type. " +
                "If multiple Related Entities are found, only first Entity is used as new Originator, other entities are discarded.<br/>" +
                "Alarm Originator found only in case original Originator is <code>Alarm</code> entity.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbTransformationNodeChangeOriginatorConfig",
        icon = "find_replace"
)
public class TbChangeOriginatorNode extends TbAbstractTransformNode {

    protected static final String CUSTOMER_SOURCE = "CUSTOMER";
    protected static final String TENANT_SOURCE = "TENANT";
    protected static final String RELATED_SOURCE = "RELATED";
    protected static final String ALARM_ORIGINATOR_SOURCE = "ALARM_ORIGINATOR";
    protected static final String ENTITY_SOURCE = "ENTITY_SOURCE";

    private TbChangeOriginatorNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbChangeOriginatorNodeConfiguration.class);
        validateConfig(config);
        setConfig(config);
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        ListenableFuture<? extends EntityId> newOriginator = getNewOriginator(ctx, msg);
        return Futures.transform(newOriginator, n -> {
            if (n == null || n.isNullUid()) {
                return null;
            }
            return Collections.singletonList((ctx.transformMsg(msg, msg.getType(), n, msg.getMetaData(), msg.getData())));
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<? extends EntityId> getNewOriginator(TbContext ctx, TbMsg msg) {
        switch (config.getOriginatorSource()) {
            case CUSTOMER_SOURCE:
                return EntitiesCustomerIdAsyncLoader.findEntityIdAsync(ctx, msg.getOriginator());
            case TENANT_SOURCE:
                return EntitiesTenantIdAsyncLoader.findEntityIdAsync(ctx, msg.getOriginator());
            case RELATED_SOURCE:
                return EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, msg.getOriginator(), config.getRelationsQuery());
            case ALARM_ORIGINATOR_SOURCE:
                return EntitiesAlarmOriginatorIdAsyncLoader.findEntityIdAsync(ctx, msg.getOriginator());
            case ENTITY_SOURCE:
                EntityType entityType = EntityType.valueOf(config.getEntityType());
                String entityName = TbNodeUtils.processPattern(config.getEntityNamePattern(), msg);
                try {
                    EntityId targetEntity = EntitiesByNameAndTypeLoader.findEntityId(ctx, entityType, entityName);
                    return Futures.immediateFuture(targetEntity);
                } catch (IllegalStateException e) {
                    return Futures.immediateFailedFuture(e);
                }
            default:
                return Futures.immediateFailedFuture(new IllegalStateException("Unexpected originator source " + config.getOriginatorSource()));
        }
    }

    private void validateConfig(TbChangeOriginatorNodeConfiguration conf) {
        HashSet<String> knownSources = Sets.newHashSet(CUSTOMER_SOURCE, TENANT_SOURCE, RELATED_SOURCE, ALARM_ORIGINATOR_SOURCE, ENTITY_SOURCE);
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

        if (conf.getOriginatorSource().equals(ENTITY_SOURCE)) {
            if (conf.getEntityType() == null) {
                log.error("Entity type not specified for [{}]", ENTITY_SOURCE);
                throw new IllegalArgumentException("Wrong config for [{}] in TbChangeOriginatorNode!" + ENTITY_SOURCE);
            }
            if (StringUtils.isEmpty(conf.getEntityNamePattern())) {
                log.error("EntityNamePattern not specified for type [{}]", conf.getEntityType());
                throw new IllegalArgumentException("Wrong config for [{}] in TbChangeOriginatorNode!" + ENTITY_SOURCE);
            }
        }

    }

    @Override
    public void destroy() {

    }
}
