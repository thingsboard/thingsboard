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

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.AlarmCommentInfo;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmSearchStatus;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "assign to customer",
        configClazz = TbAssignToCustomerNodeConfiguration.class,
        nodeDescription = "Assign Message Originator Entity to Customer",
        nodeDetails = "Finds target Customer by customer name pattern and then assign Originator Entity to this customer. " +
                "Will create new Customer if it doesn't exists and 'Create new Customer if not exists' is set to true.",
        uiResources = {"static/rulenode/rulenode-core-config.js"},
        configDirective = "tbActionNodeAssignToCustomerConfig",
        icon = "add_circle"
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
    protected void doProcessCustomerAction(TbContext ctx, TbMsg msg, CustomerId customerId) {
        processAssign(ctx, msg, customerId);
    }

    private void processAssign(TbContext ctx, TbMsg msg, CustomerId customerId) {
        EntityType originatorType = msg.getOriginator().getEntityType();
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
                ctx.tellFailure(msg, new RuntimeException("Unsupported originator type '" + originatorType +
                        "'! Only 'DEVICE', 'ASSET',  'ENTITY_VIEW' or 'DASHBOARD' types are allowed."));
                break;
        }
    }

    private void processAssignAsset(TbContext ctx, TbMsg msg, CustomerId customerId) {
        AssetId assetId = new AssetId(msg.getOriginator().getId());
        ctx.getAssetService().assignAssetToCustomer(ctx.getTenantId(), assetId, customerId);
        processAlarmsAssignAndComments(ctx, msg, assetId);
    }

    private void processAssignDevice(TbContext ctx, TbMsg msg, CustomerId customerId) {
        DeviceId deviceId = new DeviceId(msg.getOriginator().getId());
        ctx.getDeviceService().assignDeviceToCustomer(ctx.getTenantId(), deviceId, customerId);
        processAlarmsAssignAndComments(ctx, msg, deviceId);
    }

    private void processAssignEntityView(TbContext ctx, TbMsg msg, CustomerId customerId) {
        EntityViewId entityViewId = new EntityViewId(msg.getOriginator().getId());
        ctx.getEntityViewService().assignEntityViewToCustomer(ctx.getTenantId(), entityViewId, customerId);
        processAlarmsAssignAndComments(ctx, msg, entityViewId);
    }

    private void processAssignEdge(TbContext ctx, TbMsg msg, CustomerId customerId) {
        EdgeId edgeId = new EdgeId(msg.getOriginator().getId());
        ctx.getEdgeService().assignEdgeToCustomer(ctx.getTenantId(), edgeId, customerId);
        processAlarmsAssignAndComments(ctx, msg, edgeId);
    }

    private void processAssignDashboard(TbContext ctx, TbMsg msg, CustomerId customerId) {
        ctx.getDashboardService().assignDashboardToCustomer(ctx.getTenantId(), new DashboardId(msg.getOriginator().getId()), customerId);
    }

    private void processAlarmsAssignAndComments(TbContext ctx, TbMsg msg, EntityId entityId) {
        boolean unassignAlarms = Boolean.parseBoolean(msg.getMetaData().getData().getOrDefault("unassignAlarms", "True"));
        boolean removeAlarmComments = Boolean.parseBoolean(msg.getMetaData().getData().getOrDefault("removeAlarmComments", "True"));

        if (removeAlarmComments || unassignAlarms) {
            AlarmQuery alarmQuery = new AlarmQuery(entityId, new TimePageLink(Integer.MAX_VALUE),
                    AlarmSearchStatus.ANY, null, null, false);
            PageData<AlarmInfo> alarmInfoPageData = null;
            try {
                alarmInfoPageData = ctx.getAlarmService().findAlarms(ctx.getTenantId(), alarmQuery).get(10, TimeUnit.SECONDS);
                if (!alarmInfoPageData.getData().isEmpty()) {
                    for (AlarmInfo alarmInfo : alarmInfoPageData.getData()) {
                        if (unassignAlarms && alarmInfo.getAssigneeId() != null) {
                            ctx.getAlarmService().unassignAlarm(ctx.getTenantId(), alarmInfo.getId(), System.currentTimeMillis());
                        }
                        if (removeAlarmComments) {
                            PageData<AlarmCommentInfo> alarmComments =
                                    ctx.getAlarmCommentService().findAlarmComments(ctx.getTenantId(), alarmInfo.getId(), new PageLink(Integer.MAX_VALUE));
                            if (!alarmComments.getData().isEmpty()) {
                                alarmComments.getData().forEach(commentInfo -> ctx.getAlarmCommentService().deleteAlarmComment(ctx.getTenantId(), commentInfo.getId()));
                            }
                        }
                    }
                }
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                ctx.tellFailure(msg, e);
            }
        }
    }

}
