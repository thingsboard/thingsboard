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
package org.thingsboard.rule.engine.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.exception.DataValidationException;

import java.util.EnumSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.thingsboard.common.util.DonAsynchron.withCallback;

@Slf4j
public abstract class TbAbstractCustomerActionNode<C extends TbAbstractCustomerActionNodeConfiguration> implements TbNode {

    private static final Set<EntityType> supportedEntityTypes = EnumSet.of(EntityType.ASSET, EntityType.DEVICE,
            EntityType.ENTITY_VIEW, EntityType.DASHBOARD, EntityType.EDGE);

    private static final String supportedEntityTypesStr = supportedEntityTypes.stream().map(Enum::name).collect(Collectors.joining(", "));

    protected C config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = loadCustomerNodeActionConfig(configuration);
    }

    protected abstract boolean createCustomerIfNotExists();

    protected abstract C loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException;

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        var entityType = msg.getOriginator().getEntityType();
        if (!supportedEntityTypes.contains(entityType)) {
            throw new RuntimeException(unsupportedOriginatorTypeErrorMessage(entityType));
        }
        withCallback(processCustomerAction(ctx, msg),
                m -> ctx.tellSuccess(msg),
                t -> ctx.tellFailure(msg, t), MoreExecutors.directExecutor());
    }

    protected abstract ListenableFuture<Void> processCustomerAction(TbContext ctx, TbMsg msg);

    protected ListenableFuture<CustomerId> getCustomerIdFuture(TbContext ctx, TbMsg msg) {
        var tenantId = ctx.getTenantId();
        var customerTitle = TbNodeUtils.processPattern(this.config.getCustomerNamePattern(), msg);
        var customerService = ctx.getCustomerService();
        var customerByTitleFuture = customerService.findCustomerByTenantIdAndTitleAsync(tenantId, customerTitle);
        if (createCustomerIfNotExists()) {
            return Futures.transform(customerByTitleFuture, customerOpt -> {
                if (customerOpt.isPresent()) {
                    return customerOpt.get().getId();
                }
                try {
                    var newCustomer = new Customer();
                    newCustomer.setTitle(customerTitle);
                    newCustomer.setTenantId(tenantId);
                    var savedCustomer = customerService.saveCustomer(newCustomer);
                    ctx.enqueue(ctx.customerCreatedMsg(savedCustomer, ctx.getSelfId()),
                            () -> log.trace("Pushed Customer Created message: {}", savedCustomer),
                            throwable -> log.warn("Failed to push Customer Created message: {}", savedCustomer, throwable));
                    return savedCustomer.getId();
                } catch (DataValidationException e) {
                    customerOpt = customerService.findCustomerByTenantIdAndTitle(tenantId, customerTitle);
                    if (customerOpt.isPresent()) {
                        return customerOpt.get().getId();
                    }
                    throw new RuntimeException("Failed to create customer with title '" + customerTitle + "' due to: ", e);
                }
            }, MoreExecutors.directExecutor());
        }
        return Futures.transform(customerByTitleFuture, customerOpt -> {
            if (customerOpt.isEmpty()) {
                throw new NoSuchElementException("Customer with title '" + customerTitle + "' doesn't exist!");
            }
            return customerOpt.get().getId();
        }, MoreExecutors.directExecutor());
    }

    private static String unsupportedOriginatorTypeErrorMessage(EntityType originatorType) {
        return "Unsupported originator type '" + originatorType +
                "'! Only " + supportedEntityTypesStr + " types are allowed.";
    }

    @Override
    public TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) {
        boolean hasChanges = false;
        switch (fromVersion) {
            case 0 -> {
                if (oldConfiguration.has("customerCacheExpiration")) {
                    ((ObjectNode) oldConfiguration).remove("customerCacheExpiration");
                    hasChanges = true;
                }
            }
        }
        return new TbPair<>(hasChanges, oldConfiguration);
    }

}
