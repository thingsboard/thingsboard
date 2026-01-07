/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.rule.engine.util.EntitiesRelatedEntityIdAsyncLoader;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.NoSuchElementException;

import static com.google.common.util.concurrent.Futures.immediateFuture;
import static org.thingsboard.rule.engine.transform.OriginatorSource.ENTITY;
import static org.thingsboard.rule.engine.transform.OriginatorSource.RELATED;

@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "change originator",
        configClazz = TbChangeOriginatorNodeConfiguration.class,
        nodeDescription = "Change message originator to Tenant/Customer/Related Entity/Alarm Originator/Entity by name pattern.",
        nodeDetails = "Configuration: <ul><li><strong>Customer</strong> - use customer of incoming message originator as new originator. " +
                "Only for assigned to customer originators with one of the following type: 'User', 'Asset', 'Device'.</li>" +
                "<li><strong>Tenant</strong> - use current tenant as new originator.</li>" +
                "<li><strong>Related Entity</strong> - use related entity as new originator. Lookup based on configured relation query. " +
                "If multiple related entities are found, only first entity is used as new originator, other entities are discarded.</li>" +
                "<li><strong>Alarm Originator</strong> - use alarm originator as new originator. Only if incoming message originator is alarm entity.</li>" +
                "<li><strong>Entity by name pattern</strong> - specify entity type and name pattern of new originator. Following entity types are supported: " +
                "'Device', 'Asset', 'Entity View', 'Edge' or 'User'.</li></ul>" +
                "Output connections: <code>Success</code>, <code>Failure</code>.",
        configDirective = "tbTransformationNodeChangeOriginatorConfig",
        icon = "find_replace",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/transformation/change-originator/"
)
public class TbChangeOriginatorNode extends TbAbstractTransformNode<TbChangeOriginatorNodeConfiguration> {

    @Override
    protected TbChangeOriginatorNodeConfiguration loadNodeConfiguration(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbChangeOriginatorNodeConfiguration.class);
        validateConfig(config);
        return config;
    }

    @Override
    protected ListenableFuture<List<TbMsg>> transform(TbContext ctx, TbMsg msg) {
        ListenableFuture<? extends EntityId> newOriginatorFuture = getNewOriginator(ctx, msg);
        return Futures.transformAsync(newOriginatorFuture, newOriginator -> {
            if (newOriginator == null || newOriginator.isNullUid()) {
                return Futures.immediateFailedFuture(new NoSuchElementException("Failed to find new originator!"));
            }
            return immediateFuture(List.of(ctx.transformMsgOriginator(msg, newOriginator)));
        }, ctx.getDbCallbackExecutor());
    }

    private ListenableFuture<? extends EntityId> getNewOriginator(TbContext ctx, TbMsg msg) {
        switch (config.getOriginatorSource()) {
            case CUSTOMER:
                if (msg.getOriginator().getEntityType() == EntityType.CUSTOMER) {
                    return immediateFuture(msg.getOriginator());
                }
                return ctx.getEntityService().fetchEntityCustomerIdAsync(ctx.getTenantId(), msg.getOriginator())
                        .transform(customerIdOpt -> customerIdOpt.orElse(null), ctx.getDbCallbackExecutor());
            case TENANT:
                return immediateFuture(ctx.getTenantId());
            case RELATED:
                return EntitiesRelatedEntityIdAsyncLoader.findEntityAsync(ctx, msg.getOriginator(), config.getRelationsQuery());
            case ALARM_ORIGINATOR:
                return EntitiesAlarmOriginatorIdAsyncLoader.findEntityIdAsync(ctx, msg.getOriginator());
            case ENTITY:
                EntityType entityType = EntityType.valueOf(config.getEntityType());
                String entityName = TbNodeUtils.processPattern(config.getEntityNamePattern(), msg);
                try {
                    EntityId targetEntity = EntitiesByNameAndTypeLoader.findEntityId(ctx, entityType, entityName);
                    return immediateFuture(targetEntity);
                } catch (IllegalStateException e) {
                    return Futures.immediateFailedFuture(e);
                }
            default:
                return Futures.immediateFailedFuture(new IllegalStateException("Unexpected originator source " + config.getOriginatorSource()));
        }
    }

    private void validateConfig(TbChangeOriginatorNodeConfiguration conf) {
        if (conf.getOriginatorSource() == null) {
            log.debug("Originator source should be specified.");
            throw new IllegalArgumentException("Originator source should be specified.");
        }
        if (conf.getOriginatorSource().equals(RELATED) && conf.getRelationsQuery() == null) {
            log.debug("Relations query should be specified if 'Related entity' source is selected.");
            throw new IllegalArgumentException("Relations query should be specified if 'Related entity' source is selected.");
        }
        if (conf.getOriginatorSource().equals(ENTITY)) {
            if (conf.getEntityType() == null) {
                log.debug("Entity type should be specified if '{}' source is selected.", ENTITY);
                throw new IllegalArgumentException("Entity type should be specified if 'Entity by name pattern' source is selected.");
            }
            if (StringUtils.isEmpty(conf.getEntityNamePattern())) {
                log.debug("Name pattern should be specified if '{}' source is selected.", ENTITY);
                throw new IllegalArgumentException("Name pattern should be specified if 'Entity by name pattern' source is selected.");
            }
            EntitiesByNameAndTypeLoader.checkEntityType(EntityType.valueOf(conf.getEntityType()));
        }
    }

}
