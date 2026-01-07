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

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.data.script.ScriptLanguage;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.Objects;

@Slf4j
@RuleNode(
        type = ComponentType.ACTION,
        name = "log",
        configClazz = TbLogNodeConfiguration.class,
        nodeDescription = "Log incoming messages using JS script for transformation Message into String",
        nodeDetails = "Transform incoming Message with configured JS function to String and log final value into Thingsboard log file. " +
                "Message payload can be accessed via <code>msg</code> property. For example <code>'temperature = ' + msg.temperature ;</code>. " +
                "Message metadata can be accessed via <code>metadata</code> property. For example <code>'name = ' + metadata.customerName;</code>.",
        configDirective = "tbActionNodeLogConfig",
        icon = "menu",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/nodes/action/log/"
)
public class TbLogNode implements TbNode {

    private ScriptEngine scriptEngine;
    private boolean standard;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        var config = TbNodeUtils.convert(configuration, TbLogNodeConfiguration.class);
        standard = isStandard(config);
        scriptEngine = standard ? null : createScriptEngine(ctx, config);
    }

    ScriptEngine createScriptEngine(TbContext ctx, TbLogNodeConfiguration config) {
        return ctx.createScriptEngine(config.getScriptLang(),
                ScriptLanguage.TBEL.equals(config.getScriptLang()) ? config.getTbelScript() : config.getJsScript());
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        if (!log.isInfoEnabled()) {
            ctx.tellSuccess(msg);
            return;
        }
        if (standard) {
            logStandard(ctx, msg);
            return;
        }

        Futures.addCallback(scriptEngine.executeToStringAsync(msg), new FutureCallback<>() {
            @Override
            public void onSuccess(@Nullable String result) {
                log.info(result);
                ctx.tellSuccess(msg);
            }

            @Override
            public void onFailure(@NonNull Throwable t) {
                ctx.tellFailure(msg, t);
            }
        }, MoreExecutors.directExecutor()); //usually js responses runs on js callback executor
    }

    boolean isStandard(TbLogNodeConfiguration conf) {
        Objects.requireNonNull(conf, "node config is null");
        final TbLogNodeConfiguration defaultConfig = new TbLogNodeConfiguration().defaultConfiguration();

        if (conf.getScriptLang() == null || conf.getScriptLang().equals(ScriptLanguage.JS)) {
            return defaultConfig.getJsScript().equals(conf.getJsScript());
        } else if (conf.getScriptLang().equals(ScriptLanguage.TBEL)) {
            return defaultConfig.getTbelScript().equals(conf.getTbelScript());
        } else {
            log.warn("No rule to define isStandard script for script language [{}], assuming that is non-standard", conf.getScriptLang());
            return false;
        }
    }

    void logStandard(TbContext ctx, TbMsg msg) {
        log.info(toLogMessage(msg));
        ctx.tellSuccess(msg);
    }

    String toLogMessage(TbMsg msg) {
        return "\n" +
                "Incoming message:\n" + msg.getData() + "\n" +
                "Incoming metadata:\n" + JacksonUtil.toString(msg.getMetaData().getData());
    }

    @Override
    public void destroy() {
        if (scriptEngine != null) {
            scriptEngine.destroy();
        }
    }

}
