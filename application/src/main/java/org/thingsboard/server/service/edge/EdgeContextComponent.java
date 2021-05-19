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
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.edge.EdgeEventService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.rule.RuleChainService;
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
import org.thingsboard.server.service.edge.rpc.processor.RelationEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.RuleChainEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.TelemetryEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.UserEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WidgetBundleEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.processor.WidgetTypeEdgeProcessor;
import org.thingsboard.server.service.edge.rpc.sync.EdgeRequestsService;
import org.thingsboard.server.service.executors.DbCallbackExecutorService;

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
    private AlarmEdgeProcessor alarmProcessor;

    @Lazy
    @Autowired
    private DeviceProfileEdgeProcessor deviceProfileProcessor;

    @Lazy
    @Autowired
    private DeviceEdgeProcessor deviceProcessor;

    @Lazy
    @Autowired
    private EntityEdgeProcessor entityProcessor;

    @Lazy
    @Autowired
    private AssetEdgeProcessor assetProcessor;

    @Lazy
    @Autowired
    private EntityViewEdgeProcessor entityViewProcessor;

    @Lazy
    @Autowired
    private UserEdgeProcessor userProcessor;

    @Lazy
    @Autowired
    private RelationEdgeProcessor relationProcessor;

    @Lazy
    @Autowired
    private TelemetryEdgeProcessor telemetryProcessor;

    @Lazy
    @Autowired
    private DashboardEdgeProcessor dashboardProcessor;

    @Lazy
    @Autowired
    private RuleChainEdgeProcessor ruleChainProcessor;

    @Lazy
    @Autowired
    private CustomerEdgeProcessor customerProcessor;

    @Lazy
    @Autowired
    private WidgetBundleEdgeProcessor widgetBundleProcessor;

    @Lazy
    @Autowired
    private WidgetTypeEdgeProcessor widgetTypeProcessor;

    @Lazy
    @Autowired
    private AdminSettingsEdgeProcessor adminSettingsProcessor;

    @Lazy
    @Autowired
    private EdgeEventStorageSettings edgeEventStorageSettings;

    @Autowired
    @Getter
    private DbCallbackExecutorService dbCallbackExecutor;
}
