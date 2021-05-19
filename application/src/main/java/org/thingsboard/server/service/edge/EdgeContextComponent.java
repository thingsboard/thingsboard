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
package org.thingsboard.server.service.edge;

import lombok.Data;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceCredentialsService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.constructor.AdminSettingsMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AlarmMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomerMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceProfileMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetTypeMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetsBundleMsgConstructor;
import org.thingsboard.server.service.edge.rpc.processor.AdminSettingsProcessor;
import org.thingsboard.server.service.edge.rpc.processor.AssetProcessor;
import org.thingsboard.server.service.edge.rpc.processor.CustomerProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DashboardProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DeviceProfileProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EntityProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EntityViewProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RuleChainProcessor;
import org.thingsboard.server.service.edge.rpc.processor.UserProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WidgetBundleProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WidgetTypeProcessor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.edge.rpc.processor.AlarmProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DeviceProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RelationProcessor;
import org.thingsboard.server.service.edge.rpc.processor.TelemetryProcessor;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.queue.TbClusterService;
import org.thingsboard.server.service.state.DeviceStateService;

@Component
@TbCoreComponent
@Data
public class EdgeContextComponent {

    @Lazy
    @Autowired
    private EdgeService edgeService;

    @Lazy
    @Autowired
    private EdgeEventService edgeEventService;

    @Lazy
    @Autowired
    private AssetService assetService;

    @Lazy
    @Autowired
    private DeviceProfileService deviceProfileService;

    @Lazy
    @Autowired
    private AttributesService attributesService;

    @Lazy
    @Autowired
    private DashboardService dashboardService;

    @Lazy
    @Autowired
    private RuleChainService ruleChainService;

    @Lazy
    @Autowired
    private UserService userService;

    @Lazy
    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Lazy
    @Autowired
    private EdgeRequestsService edgeRequestsService;

    @Lazy
    @Autowired
    private AlarmProcessor alarmProcessor;

    @Lazy
    @Autowired
    private DeviceProfileProcessor deviceProfileProcessor;

    @Lazy
    @Autowired
    private DeviceProcessor deviceProcessor;

    @Lazy
    @Autowired
    private EntityProcessor entityProcessor;

    @Lazy
    @Autowired
    private AssetProcessor assetProcessor;

    @Lazy
    @Autowired
    private EntityViewProcessor entityViewProcessor;

    @Lazy
    @Autowired
    private UserProcessor userProcessor;

    @Lazy
    @Autowired
    private RelationProcessor relationProcessor;

    @Lazy
    @Autowired
    private TelemetryProcessor telemetryProcessor;

    @Lazy
    @Autowired
    private DashboardProcessor dashboardProcessor;

    @Lazy
    @Autowired
    private RuleChainProcessor ruleChainProcessor;

    @Lazy
    @Autowired
    private CustomerProcessor customerProcessor;

    @Lazy
    @Autowired
    private WidgetBundleProcessor widgetBundleProcessor;

    @Lazy
    @Autowired
    private WidgetTypeProcessor widgetTypeProcessor;

    @Lazy
    @Autowired
    private AdminSettingsProcessor adminSettingsProcessor;

    @Lazy
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;
}
