/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
