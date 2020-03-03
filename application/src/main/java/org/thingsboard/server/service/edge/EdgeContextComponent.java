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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.thingsboard.server.actors.service.ActorService;
import org.thingsboard.server.dao.alarm.AlarmService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.dashboard.DashboardService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.relation.RelationService;

@Component
@Data
public class EdgeContextComponent {

    @Lazy
    @Autowired
    private EdgeService edgeService;

    @Lazy
    @Autowired
    private AssetService assetService;

    @Lazy
    @Autowired
    private DeviceService deviceService;

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
    private ActorService actorService;
}