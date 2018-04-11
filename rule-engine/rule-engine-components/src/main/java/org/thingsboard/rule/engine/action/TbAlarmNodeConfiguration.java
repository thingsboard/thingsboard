/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

@Data
public class TbAlarmNodeConfiguration implements NodeConfiguration {

    private String createConditionJs;
    private String clearConditionJs;
    private String alarmDetailsBuildJs;
    private String alarmType;
    private AlarmSeverity severity;
    private boolean propagate;


    @Override
    public TbAlarmNodeConfiguration defaultConfiguration() {
        TbAlarmNodeConfiguration configuration = new TbAlarmNodeConfiguration();
        configuration.setCreateConditionJs("return 'incoming message = ' + msg + meta;");
        configuration.setClearConditionJs("return 'incoming message = ' + msg + meta;");
        configuration.setAlarmDetailsBuildJs("return 'incoming message = ' + msg + meta;");
        configuration.setAlarmType("General Alarm");
        configuration.setSeverity(AlarmSeverity.CRITICAL);
        configuration.setPropagate(false);
        return configuration;
    }
}
