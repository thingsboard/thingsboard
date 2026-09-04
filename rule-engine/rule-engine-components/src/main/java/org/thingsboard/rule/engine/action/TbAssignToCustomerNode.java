// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.action;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

@RuleNode(
        type = ComponentType.ACTION,
        name = "assign to customer",
        configClazz = TbAssignToCustomerNodeConfiguration.class,
        nodeDescription = "Assign message originator entity to customer",
        nodeDetails = "Finds target customer by title and assign message originator entity to this customer. " +
                "Rule node will create a new customer if it doesn't exist, and 'Create new customer if it doesn't exist' enabled.",
        configDirective = "tbActionNodeAssignToCustomerConfig",
        icon = "add_circle",
        version = 1,
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/assign-to-customer/"
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
        return Futures.transform(customerIdFuture, customerId -> {
            var originator = msg.getOriginator();
            switch (originator.getEntityType()) {
                case ASSET ->
                        ctx.getAssetService().assignAssetToCustomer(ctx.getTenantId(), new AssetId(originator.getId()), customerId);
                case DEVICE ->
                        ctx.getDeviceService().assignDeviceToCustomer(ctx.getTenantId(), new DeviceId(originator.getId()), customerId);
                case ENTITY_VIEW ->
                        ctx.getEntityViewService().assignEntityViewToCustomer(ctx.getTenantId(), new EntityViewId(originator.getId()), customerId);
                case EDGE ->
                        ctx.getEdgeService().assignEdgeToCustomer(ctx.getTenantId(), new EdgeId(originator.getId()), customerId);
                case DASHBOARD ->
                        ctx.getDashboardService().assignDashboardToCustomer(ctx.getTenantId(), new DashboardId(originator.getId()), customerId);
            }
            return null;
        }, MoreExecutors.directExecutor());
    }

}
