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
package org.thingsboard.server.service.alarm.rule;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.RuleEngineAlarmService;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.cluster.TbClusterService;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.attributes.AttributesService;
import org.thingsboard.server.dao.device.DeviceService;
import org.thingsboard.server.dao.timeseries.TimeseriesService;
import org.thingsboard.server.queue.util.TbRuleEngineComponent;
import org.thingsboard.server.service.alarm.rule.store.AlarmRuleEntityStateStore;

@Getter
@RequiredArgsConstructor
@Service
@TbRuleEngineComponent
public class TbAlarmRuleContext {
    private final DeviceService deviceService;
    private final AssetService assetService;
    private final AttributesService attributesService;
    private final TimeseriesService timeseriesService;
    private final RuleEngineAlarmService alarmService;
    private final TbClusterService clusterService;
    private final ActorSystemContext actorSystemContext;
    private final AlarmRuleEntityStateStore stateStore;
}
