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
package org.thingsboard.server.service.edge;

import freemarker.template.Configuration;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.queue.QueueService;
import org.thingsboard.server.dao.rule.RuleChainService;
import org.thingsboard.server.dao.settings.AdminSettingsService;
import org.thingsboard.server.dao.user.UserService;
import org.thingsboard.server.dao.widget.WidgetsBundleService;
import org.thingsboard.server.queue.util.TbCoreComponent;
import org.thingsboard.server.service.edge.rpc.EdgeEventStorageSettings;
import org.thingsboard.server.service.edge.rpc.processor.AdminSettingsEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.AlarmEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.AssetEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.CustomerEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DashboardEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DeviceEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.DeviceProfileEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EntityEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.EntityViewEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.OtaPackageEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.QueueEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.TelemetryEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WidgetTypeEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;
import org.thingsboard.server.service.executors.GrpcCallbackExecutorService;

@Component
@TbCoreComponent
@Data
@Lazy
public class EdgeContextComponent {

    @Autowired
    private TbClusterService clusterService;

    @Autowired
    private EdgeService edgeService;

    @Autowired
    private EdgeEventService edgeEventService;

    @Autowired
    private AdminSettingsService adminSettingsService;

    @Autowired
    private Configuration freemarkerConfig;

    @Autowired
    private AssetService assetService;

    @Autowired
    private DeviceProfileService deviceProfileService;

    @Autowired
    private AttributesService attributesService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private RuleChainService ruleChainService;

    @Autowired
    private UserService userService;

    @Autowired
    private WidgetsBundleService widgetsBundleService;

    @Autowired
    private EdgeRequestsService edgeRequestsService;

    @Autowired
    private OtaPackageService otaPackageService;

    @Autowired
    private QueueService queueService;

    @Autowired
    private AlarmEdgeProcessor alarmProcessor;

    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    @Autowired
    private EntityEdgeProcessor entityProcessor;

    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    @Autowired
    private UserEdgeProcessor userProcessor;

    @Autowired
    private RelationEdgeProcessor relationProcessor;

    @Autowired
    private TelemetryEdgeProcessor telemetryProcessor;

    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Autowired
    private AdminSettingsEdgeProcessor adminSettingsProcessor;

    @Autowired
    private OtaPackageEdgeProcessor otaPackageEdgeProcessor;

    @Autowired
    private QueueEdgeProcessor queueEdgeProcessor;

    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Autowired
    private DbCallbackExecutorService dbCallbackExecutor;

    @Autowired
    private GrpcCallbackExecutorService grpcCallbackExecutorService;
}
