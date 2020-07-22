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
package org.thingsboard.server.service.telemetry.cmd;

import lombok.Data;
import org.thingsboard.server.service.telemetry.cmd.v1.AttributesSubscriptionCmd;
import org.thingsboard.server.service.telemetry.cmd.v1.GetHistoryCmd;
import org.thingsboard.server.service.telemetry.cmd.v1.TimeseriesSubscriptionCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.AlarmDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.AlarmDataUnsubscribeCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataCmd;
import org.thingsboard.server.service.telemetry.cmd.v2.EntityDataUnsubscribeCmd;

import java.util.List;

/**
 * @author Andrew Shvayka
 */
@Data
public class TelemetryPluginCmdsWrapper {

    private List<AttributesSubscriptionCmd> attrSubCmds;

    private List<TimeseriesSubscriptionCmd> tsSubCmds;

    private List<GetHistoryCmd> historyCmds;

    private List<EntityDataCmd> entityDataCmds;

    private List<EntityDataUnsubscribeCmd> entityDataUnsubscribeCmds;

    private List<AlarmDataCmd> alarmDataCmds;

    private List<AlarmDataUnsubscribeCmd> alarmDataUnsubscribeCmds;

}
