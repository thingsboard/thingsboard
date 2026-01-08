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
