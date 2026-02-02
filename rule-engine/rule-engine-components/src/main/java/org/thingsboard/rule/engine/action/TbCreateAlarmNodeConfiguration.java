/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.data.validation.NoXss;

import java.util.Collections;
import java.util.List;

@Data
public class TbCreateAlarmNodeConfiguration extends TbAbstractAlarmNodeConfiguration implements NodeConfiguration<TbCreateAlarmNodeConfiguration> {

    @NoXss
    private String severity;
    private boolean propagate;
    private boolean propagateToOwner;
    private boolean propagateToTenant;
    private boolean useMessageAlarmData;
    private boolean overwriteAlarmDetails = true;
    private boolean dynamicSeverity;

    private List<String> relationTypes;

    @Override
    public TbCreateAlarmNodeConfiguration defaultConfiguration() {
        TbCreateAlarmNodeConfiguration configuration = new TbCreateAlarmNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setAlarmDetailsBuildJs(ALARM_DETAILS_BUILD_JS_TEMPLATE);
        configuration.setAlarmDetailsBuildTbel(ALARM_DETAILS_BUILD_TBEL_TEMPLATE);
        configuration.setAlarmType("General Alarm");
        configuration.setSeverity(AlarmSeverity.CRITICAL.name());
        configuration.setPropagate(false);
        configuration.setPropagateToOwner(false);
        configuration.setPropagateToTenant(false);
        configuration.setUseMessageAlarmData(false);
        configuration.setOverwriteAlarmDetails(false);
        configuration.setRelationTypes(Collections.emptyList());
        configuration.setDynamicSeverity(false);
        return configuration;
    }

}
