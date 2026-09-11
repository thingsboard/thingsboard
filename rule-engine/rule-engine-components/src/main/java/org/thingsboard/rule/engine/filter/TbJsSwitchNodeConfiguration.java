// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.filter;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.script.ScriptLanguage;

@Data
public class TbJsSwitchNodeConfiguration implements NodeConfiguration<TbJsSwitchNodeConfiguration> {

    private static final String DEFAULT_JS_SCRIPT = "function nextRelation(metadata, msg) {\n" +
            "    return ['one','nine'];\n" +
            "}\n" +
            "if(msgType === 'POST_TELEMETRY_REQUEST') {\n" +
            "    return ['two'];\n" +
            "}\n" +
            "return nextRelation(metadata, msg);";

    private static final String DEFAULT_TBEL_SCRIPT = "function nextRelation(metadata, msg) {\n" +
            "    return ['one','nine'];\n" +
            "}\n" +
            "if(msgType == 'POST_TELEMETRY_REQUEST') {\n" +
            "    return ['two'];\n" +
            "}\n" +
            "return nextRelation(metadata, msg);";

    private ScriptLanguage scriptLang;
    private String jsScript;
    private String tbelScript;

    @Override
    public TbJsSwitchNodeConfiguration defaultConfiguration() {
        TbJsSwitchNodeConfiguration configuration = new TbJsSwitchNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setJsScript(DEFAULT_JS_SCRIPT);
        configuration.setTbelScript(DEFAULT_TBEL_SCRIPT);
        return configuration;
    }
}
