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
package org.thingsboard.rule.engine.action;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;

import java.util.Collections;
import java.util.List;

@Data
public class TbCreateAlarmNodeConfiguration extends TbAbstractAlarmNodeConfiguration implements NodeConfiguration<TbCreateAlarmNodeConfiguration> {

    private AlarmSeverity severity;
    private boolean propagate;
    private boolean useMessageAlarmData;

    private List<String> relationTypes;

    @Override
    public TbCreateAlarmNodeConfiguration defaultConfiguration() {
        TbCreateAlarmNodeConfiguration configuration = new TbCreateAlarmNodeConfiguration();
        configuration.setAlarmDetailsBuildJs("var details = {};\n" +
                "if (metadata.prevAlarmDetails) {\n" +
                "    details = JSON.parse(metadata.prevAlarmDetails);\n" +
                "}\n" +
                "return details;");
        configuration.setAlarmType("General Alarm");
        configuration.setSeverity(AlarmSeverity.CRITICAL);
        configuration.setPropagate(false);
        configuration.setUseMessageAlarmData(false);
        configuration.setRelationTypes(Collections.emptyList());
        return configuration;
    }
}
