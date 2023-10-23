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
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ACTION,
        name = "unassign from customer",
        configClazz = TbUnassignFromCustomerNodeConfiguration.class,
        nodeDescription = "Unassign message originator entity from customer",
        nodeDetails = "Finds target customer by title and then unassign originator entity from this customer.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeUnAssignToCustomerConfig",
        icon = "remove_circle",
        version = 1
)
public class TbUnassignFromCustomerNode extends TbAbstractCustomerActionNode<TbUnassignFromCustomerNodeConfiguration> {

    @Override
    protected boolean createCustomerIfNotExists() {
        return false;
    }

    @Override
    protected TbUnassignFromCustomerNodeConfiguration loadCustomerNodeActionConfig(TbNodeConfiguration configuration) throws TbNodeException {
        return TbNodeUtils.convert(configuration, TbUnassignFromCustomerNodeConfiguration.class);
    }

    @Override
    protected ListenableFuture<Void> processCustomerAction(TbContext ctx, TbMsg msg) {
        var originatorType = msg.getOriginator().getEntityType();
        if (EntityType.DASHBOARD.equals(originatorType)) {
            if (StringUtils.isEmpty(config.getCustomerNamePattern())) {
                throw new RuntimeException("Failed to unassign dashboard with id '" +
                        msg.getOriginator().getId() + "' from customer! Customer title should be specified!");
            }
            var customerIdFuture = getCustomerIdFuture(ctx, msg);
            return Futures.transformAsync(customerIdFuture, customerId ->
                    ctx.getDbCallbackExecutor().submit(() -> {
                        processUnnasignDashboard(ctx, msg, customerId);
                        return null;
                    }), MoreExecutors.directExecutor());
        }
        return ctx.getDbCallbackExecutor().submit(() -> {
            switch (originatorType) {
                case DEVICE:
                    processUnnasignDevice(ctx, msg);
                    break;
                case ASSET:
                    processUnnasignAsset(ctx, msg);
                    break;
                case ENTITY_VIEW:
                    processUnassignEntityView(ctx, msg);
                    break;
                case EDGE:
                    processUnassignEdge(ctx, msg);
                    break;
                default:
                    throw new RuntimeException(unsupportedOriginatorTypeErrorMessage(originatorType));
            }
            return null;
        });
    }

    private void processUnnasignAsset(TbContext ctx, TbMsg msg) {
        ctx.getAssetService().unassignAssetFromCustomer(ctx.getTenantId(), new AssetId(msg.getOriginator().getId()));
    }

    private void processUnnasignDevice(TbContext ctx, TbMsg msg) {
        ctx.getDeviceService().unassignDeviceFromCustomer(ctx.getTenantId(), new DeviceId(msg.getOriginator().getId()));
    }

    private void processUnnasignDashboard(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDashboardService().unassignDashboardFromCustomer(ctx.getTenantId(), new DashboardId(msg.getOriginator().getId()), customerId);
    }

    private void processUnassignEntityView(TbContext ctx, TbMsg msg) {
        ctx.getEntityViewService().unassignEntityViewFromCustomer(ctx.getTenantId(), new EntityViewId(msg.getOriginator().getId()));
    }

    private void processUnassignEdge(TbContext ctx, TbMsg msg) {
        ctx.getEdgeService().unassignEdgeFromCustomer(ctx.getTenantId(), new EdgeId(msg.getOriginator().getId()));
    }
}
