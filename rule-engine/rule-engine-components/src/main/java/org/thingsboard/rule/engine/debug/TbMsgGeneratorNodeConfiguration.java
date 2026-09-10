// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
