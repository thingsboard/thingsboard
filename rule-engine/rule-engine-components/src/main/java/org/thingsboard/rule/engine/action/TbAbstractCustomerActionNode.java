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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Map;
import java.util.Optional;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractCustomerActionNode<C extends TbAbstractCustomerActionNodeConfiguration> implements TbNode {

    protected final Map<EntityType, CustomerAssigner> assignersMap = Map.of(
            EntityType.ASSET, this::processAsset,
            EntityType.DEVICE, this::processDevice,
            EntityType.ENTITY_VIEW, this::processEntityView,
            EntityType.DASHBOARD, this::processDashboard,
            EntityType.EDGE, this::processEdge
    );

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadCustomerNodeActionConfig(configuration);
    }

    protected abstract boolean createCustomerIfNotExists();

    protected abstract C loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        EntityType entityType = msg.getOriginator().getEntityType();
        boolean originatorSupportedByNode = assignersMap.containsKey(entityType);
        if (!originatorSupportedByNode) {
            throw new RuntimeException(unsupportedOriginatorTypeErrorMessage(entityType));
        }
        withCallback(processCustomerAction(ctx, msg),
                m -> ctx.tellSuccess(msg),
                t -> ctx.tellFailure(msg, t), ctx.getDbCallbackExecutor());
    }

    protected abstract ListenableFuture<Void> processCustomerAction(TbContext ctx, TbMsg msg);

    protected ListenableFuture<CustomerId> getCustomerIdFuture(TbContext ctx, TbMsg msg) {
        String customerTitle = TbNodeUtils.processPattern(this.config.getCustomerNamePattern(), msg);
        return ctx.getDbCallbackExecutor().submit(() -> {
            var customerOptional = Optional.ofNullable(ctx.getCustomerService().findCustomerByTenantIdAndTitleUsingCache(ctx.getTenantId(), customerTitle));
            if (customerOptional.isPresent()) {
                return customerOptional.get().getId();
            }
            if (createCustomerIfNotExists()) {
                var newCustomer = new Customer();
                newCustomer.setTitle(customerTitle);
                newCustomer.setTenantId(ctx.getTenantId());
                Customer savedCustomer = ctx.getCustomerService().saveCustomer(newCustomer);
                ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
                        () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
                        throwable -> log.warn("Failed to push Customer Created message: {}", savedCustomer, throwable));
                return savedCustomer.getId();
            }
            throw new RuntimeException("No customer found with name '" + customerTitle + "'.");
        });
    }

    protected static String unsupportedOriginatorTypeErrorMessage(EntityType originatorType) {
        return "Unsupported originator type '" + originatorType +
                "'! Only 'DEVICE', 'ASSET', 'ENTITY_VIEW', 'DASHBOARD' types are allowed.";
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0: {
                if (oldConfiguration.has("customerCacheExpiration")) {
                    ((ObjectNode) oldConfiguration).remove("customerCacheExpiration");
                    hasChanges = true;
                }
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

    protected abstract void processAsset(TbContext ctx, EntityId originator, CustomerId customerId);

    protected abstract void processDevice(TbContext ctx, EntityId originator, CustomerId customerId);

    protected abstract void processEntityView(TbContext ctx, EntityId originator, CustomerId customerId);

    protected abstract void processDashboard(TbContext ctx, EntityId originator, CustomerId customerId);

    protected abstract void processEdge(TbContext ctx, EntityId originator, CustomerId customerId);

    @FunctionalInterface
    interface CustomerAssigner {

        void apply(TbContext ctx, EntityId originator, CustomerId customerId);

    }

}
