// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.action;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.script.ScriptLanguage;

@Data
@EqualsAndHashCode(callSuper = true)
public class TbClearAlarmNodeConfiguration extends TbAbstractAlarmNodeConfiguration implements NodeConfiguration<TbClearAlarmNodeConfiguration> {

    @Override
    public TbClearAlarmNodeConfiguration defaultConfiguration() {
        TbClearAlarmNodeConfiguration configuration = new TbClearAlarmNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setAlarmDetailsBuildJs(ALARM_DETAILS_BUILD_JS_TEMPLATE);
        configuration.setAlarmDetailsBuildTbel(ALARM_DETAILS_BUILD_TBEL_TEMPLATE);
        configuration.setAlarmType("General Alarm");
        return configuration;
    }
}
