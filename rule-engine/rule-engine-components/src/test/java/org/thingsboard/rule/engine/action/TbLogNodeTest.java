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

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.msg.TbMsgType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@Slf4j
public class TbLogNodeTest {

    @Test
    void givenMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        String data = "{\"key\": \"value\"}";
        TbMsgMetaData metaData = new TbMsgMetaData(Map.of("mdKey1", "mdValue1", "mdKey2", "23"));
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TenantId.SYS_TENANT_ID)
                .copyMetaData(metaData)
                .data(data)
                .build();

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "{\"key\": \"value\"}\n" +
                "Incoming metadata:\n" +
                "{\"mdKey1\":\"mdValue1\",\"mdKey2\":\"23\"}");
    }

    @Test
    void givenEmptyDataMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TenantId.SYS_TENANT_ID)
                .copyMetaData(metaData)
                .data("")
                .build();

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "\n" +
                "Incoming metadata:\n" +
                "{}");
    }

    @Test
    void givenNullDataMsg_whenToLog_thenReturnString() {
        TbLogNode node = new TbLogNode();
        TbMsgMetaData metaData = new TbMsgMetaData(Collections.emptyMap());
        TbMsg msg = TbMsg.newMsg()
                .type(TbMsgType.POST_TELEMETRY_REQUEST)
                .originator(TenantId.SYS_TENANT_ID)
                .copyMetaData(metaData)
                .data(null)
                .build();

        String logMessage = node.toLogMessage(msg);
        log.info(logMessage);

        assertThat(logMessage).isEqualTo("\n" +
                "Incoming message:\n" +
                "null\n" +
                "Incoming metadata:\n" +
                "{}");
    }

    @ParameterizedTest
    @EnumSource(ScriptLanguage.class)
    void givenDefaultConfig_whenIsStandardForEachScriptLanguage_thenTrue(ScriptLanguage scriptLanguage) throws TbNodeException {

        TbLogNodeConfiguration config = new TbLogNodeConfiguration().defaultConfiguration();
        config.setScriptLang(scriptLanguage);
        TbLogNode node = spy(new TbLogNode());
        TbNodeConfiguration tbNodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        TbContext ctx = mock(TbContext.class);
        node.init(ctx, tbNodeConfiguration);

        assertThat(node.isStandard(config)).as("Script is standard for language " + scriptLanguage).isTrue();
        verify(node, never()).createScriptEngine(any(), any());
        verify(ctx, never()).createScriptEngine(any(), anyString());

    }

    @Test
    void backwardCompatibility_whenScriptLangIsNull() throws TbNodeException {
        TbLogNodeConfiguration config = new TbLogNodeConfiguration().defaultConfiguration();
        TbLogNode node = spy(new TbLogNode());
        TbNodeConfiguration tbNodeConfiguration = new TbNodeConfiguration(JacksonUtil.valueToTree(config));
        TbContext ctx = mock(TbContext.class);
        node.init(ctx, tbNodeConfiguration);

        assertThat(node.isStandard(config)).as("Script is standard for language JS").isTrue();
        verify(node, never()).createScriptEngine(any(), any());
        verify(ctx, never()).createScriptEngine(any(), anyString());
    }

    @Test
    void givenScriptEngineEnum_whenNewAdded_thenFailed() {
        assertThat(ScriptLanguage.values().length).as("only two ScriptLanguage supported").isEqualTo(2);
    }

    @Test
    void givenScriptEngineLangJs_whenCreateScriptEngine_thenSupplyJsScript(){
        TbLogNodeConfiguration configJs = new TbLogNodeConfiguration().defaultConfiguration();
        configJs.setScriptLang(ScriptLanguage.JS);
        configJs.setJsScript(configJs.getJsScript() + " // This is JS script " + UUID.randomUUID());
        TbLogNode node = new TbLogNode();
        TbContext ctx = mock(TbContext.class);
        node.createScriptEngine(ctx, configJs);
        verify(ctx).createScriptEngine(ScriptLanguage.JS, configJs.getJsScript());
        verifyNoMoreInteractions(ctx);
    }

    @Test
    void givenScriptEngineLangTbel_whenCreateScriptEngine_thenSupplyTbelScript(){
        TbLogNodeConfiguration configTbel = new TbLogNodeConfiguration().defaultConfiguration();
        configTbel.setScriptLang(ScriptLanguage.TBEL);
        configTbel.setTbelScript(configTbel.getTbelScript() + " // This is TBEL script " + UUID.randomUUID());
        TbLogNode node = new TbLogNode();
        TbContext ctx = mock(TbContext.class);
        node.createScriptEngine(ctx, configTbel);
        verify(ctx).createScriptEngine(ScriptLanguage.TBEL, configTbel.getTbelScript());
        verifyNoMoreInteractions(ctx);
    }

}
