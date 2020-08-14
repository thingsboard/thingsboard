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
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetTypeService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.discovery.PartitionService;
import org.thingsboard.server.queue.provider.TbQueueProducerProvider;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.constructor.AdminSettingsUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AlarmUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.AssetUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.CustomerUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DashboardUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.DeviceUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityDataMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.EntityViewUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RelationUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.RuleChainUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.UserUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetTypeUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.constructor.WidgetsBundleUpdateMsgConstructor;
import org.thingsboard.server.service.edge.rpc.init.SyncEdgeService;
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

    @Autowired
    private PartitionService partitionService;

    @Autowired
    @Lazy
    private TbQueueProducerProvider producerProvider;

    @Lazy
    @Autowired
    private EdgeNotificationService edgeNotificationService;

    @Lazy
    @Autowired
    private AssetService assetService;

    @Lazy
    @Autowired
    private DeviceService deviceService;

    @Lazy
    @Autowired
    private DeviceCredentialsService deviceCredentialsService;

    @Lazy
    @Autowired
    private EntityViewService entityViewService;

    @Lazy
    @Autowired
    private AttributesService attributesService;

    @Lazy
    @Autowired
    private CustomerService customerService;

    @Lazy
    @Autowired
    private RelationService relationService;

    @Lazy
    @Autowired
    private AlarmService alarmService;

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
    private ActorService actorService;

    @Lazy
    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Lazy
    @Autowired
    private WidgetTypeService widgetTypeService;

    @Lazy
    @Autowired
    private DeviceStateService deviceStateService;

    @Lazy
    @Autowired
    private TbClusterService tbClusterService;

    @Lazy
    @Autowired
    private SyncEdgeService syncEdgeService;

    @Lazy
    @Autowired
    private RuleChainUpdateMsgConstructor ruleChainUpdateMsgConstructor;

    @Lazy
    @Autowired
    private AlarmUpdateMsgConstructor alarmUpdateMsgConstructor;

    @Lazy
    @Autowired
    private DeviceUpdateMsgConstructor deviceUpdateMsgConstructor;

    @Lazy
    @Autowired
    private AssetUpdateMsgConstructor assetUpdateMsgConstructor;

    @Lazy
    @Autowired
    private EntityViewUpdateMsgConstructor entityViewUpdateMsgConstructor;

    @Lazy
    @Autowired
    private DashboardUpdateMsgConstructor dashboardUpdateMsgConstructor;

    @Lazy
    @Autowired
    private CustomerUpdateMsgConstructor customerUpdateMsgConstructor;

    @Lazy
    @Autowired
    private UserUpdateMsgConstructor userUpdateMsgConstructor;

    @Lazy
    @Autowired
    private RelationUpdateMsgConstructor relationUpdateMsgConstructor;

    @Lazy
    @Autowired
    private WidgetsBundleUpdateMsgConstructor widgetsBundleUpdateMsgConstructor;

    @Lazy
    @Autowired
    private WidgetTypeUpdateMsgConstructor widgetTypeUpdateMsgConstructor;

    @Lazy
    @Autowired
    private AdminSettingsUpdateMsgConstructor adminSettingsUpdateMsgConstructor;

    @Lazy
    @Autowired
    private EntityDataMsgConstructor entityDataMsgConstructor;

    @Lazy
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;
}
