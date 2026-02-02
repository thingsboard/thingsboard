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
package org.thingsboard.rule.engine.debug;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.script.ScriptLanguage;

@Data
public class TbMsgGeneratorNodeConfiguration implements NodeConfiguration<TbMsgGeneratorNodeConfiguration> {

    public static final int UNLIMITED_MSG_COUNT = 0;
    public static final String DEFAULT_SCRIPT = "var msg = { temp: 42, humidity: 77 };\n" +
            "var metadata = { data: 40 };\n" +
            "var msgType = \"POST_TELEMETRY_REQUEST\";\n\n" +
            "return { msg: msg, metadata: metadata, msgType: msgType };";

    private int msgCount;
    private int periodInSeconds;
    private String originatorId;
    private EntityType originatorType;
    private ScriptLanguage scriptLang;
    private String jsScript;
    private String tbelScript;

    @Override
    public TbMsgGeneratorNodeConfiguration defaultConfiguration() {
        TbMsgGeneratorNodeConfiguration configuration = new TbMsgGeneratorNodeConfiguration();
        configuration.setMsgCount(UNLIMITED_MSG_COUNT);
        configuration.setPeriodInSeconds(1);
        configuration.setOriginatorType(EntityType.RULE_NODE);
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setJsScript(DEFAULT_SCRIPT);
        configuration.setTbelScript(DEFAULT_SCRIPT);
        return configuration;
    }
}
