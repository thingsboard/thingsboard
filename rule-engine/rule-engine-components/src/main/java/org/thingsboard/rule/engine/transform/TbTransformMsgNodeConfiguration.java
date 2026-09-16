// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.transform;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;
import org.thingsboard.server.common.data.script.ScriptLanguage;

@Data
public class TbTransformMsgNodeConfiguration implements NodeConfiguration<TbTransformMsgNodeConfiguration> {

    private ScriptLanguage scriptLang;
    private String jsScript;
    private String tbelScript;

    @Override
    public TbTransformMsgNodeConfiguration defaultConfiguration() {
        TbTransformMsgNodeConfiguration configuration = new TbTransformMsgNodeConfiguration();
        configuration.setScriptLang(ScriptLanguage.TBEL);
        configuration.setJsScript("return {msg: msg, metadata: metadata, msgType: msgType};");
        configuration.setTbelScript("return {msg: msg, metadata: metadata, msgType: msgType};");
        return configuration;
    }
}
