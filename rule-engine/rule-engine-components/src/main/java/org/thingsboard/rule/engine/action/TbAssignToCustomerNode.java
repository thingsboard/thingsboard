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

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "assign to customer",
        configClazz = TbAssignToCustomerNodeConfiguration.class,
        nodeDescription = "Assign message originator entity to customer",
        nodeDetails = "Finds target customer by title and assign message originator entity to this customer. " +
                "Will create a new customer if it doesn't exist, and 'Create new customer if it doesn't exist' enabled.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAssignToCustomerConfig",
        icon = "add_circle",
        version = 1
)
public class TbAssignToCustomerNode extends TbAbstractCustomerActionNode<TbAssignToCustomerNodeConfiguration> {

    @Override
    protected boolean createCustomerIfNotExists() {
        return config.isCreateCustomerIfNotExists();
    }

    @Override
    protected TbAssignToCustomerNodeConfiguration loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbAssignToCustomerNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<Void> processCustomerAction(TbContext ctx, TbMsg msg) {
        var customerIdFuture = getCustomerIdFuture(ctx, msg);
        return Futures.transformAsync(customerIdFuture, customerId ->
                ctx.getDbCallbackExecutor().submit(() -> {
                    var originatorType = msg.getOriginator().getEntityType();
                    switch (originatorType) {
                        case DEVICE:
                            processAssignDevice(ctx, msg, customerId);
                            break;
                        case ASSET:
                            processAssignAsset(ctx, msg, customerId);
                            break;
                        case ENTITY_VIEW:
                            processAssignEntityView(ctx, msg, customerId);
                            break;
                        case EDGE:
                            processAssignEdge(ctx, msg, customerId);
                            break;
                        case DASHBOARD:
                            processAssignDashboard(ctx, msg, customerId);
                            break;
                        default:
                            throw new RuntimeException(unsupportedOriginatorTypeErrorMessage(originatorType));
                    }
                    return null;
                }), MoreExecutors.directExecutor());
    }

    private void processAssignAsset(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getAssetService().assignAssetToCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()), customerId);
    }

    private void processAssignDevice(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDeviceService().assignDeviceToCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()), customerId);
    }

    private void processAssignEntityView(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getEntityViewService().assignEntityViewToCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()), customerId);
    }

    private void processAssignEdge(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getEdgeService().assignEdgeToCustomer(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId()), customerId);
    }

    private void processAssignDashboard(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDashboardService().assignDashboardToCustomer(ctx.getTenantId(), new DashboardId(msg.getOriginator().getId()), customerId);
    }

}
