/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.rule.engine.js;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.thingsboard.server.common.msg.TbMsg;

import javax.script.*;
import java.util.Map;


@Slf4j
public class NashornJsEngine {

    public static final String METADATA = "meta";
    public static final String DATA = "msg";
    private static NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

    private CompiledScript engine;

    public NashornJsEngine(String script) {
        engine = compileScript(script);
    }

    private static CompiledScript compileScript(String script) {
        ScriptEngine engine = factory.getScriptEngine(new String[]{"--no-java"});
        Compilable compEngine = (Compilable) engine;
        try {
            return compEngine.compile(script);
        } catch (ScriptException e) {
            log.warn("Failed to compile JS script: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Can't compile script: " + e.getMessage());
        }
    }

    public static Bindings bindMsg(TbMsg msg) {
        try {
            Bindings bindings = new SimpleBindings();
            bindings.put(METADATA, msg.getMetaData().getData());

            if (ArrayUtils.isNotEmpty(msg.getData())) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(msg.getData());
                Map map = mapper.treeToValue(jsonNode, Map.class);
                bindings.put(DATA, map);
            }

            return bindings;
        } catch (Throwable th) {
            throw new IllegalArgumentException("Cannot bind js args", th);
        }
    }

    public boolean executeFilter(Bindings bindings) throws ScriptException {
        Object eval = engine.eval(bindings);
        if (eval instanceof Boolean) {
            return (boolean) eval;
        } else {
            log.warn("Wrong result type: {}", eval);
            throw new ScriptException("Wrong result type: " + eval);
        }
    }

    public String executeSwitch(Bindings bindings) throws ScriptException, NoSuchMethodException {
        Object eval = this.engine.eval(bindings);
        if (eval instanceof String) {
            return (String) eval;
        } else {
            log.warn("Wrong result type: {}", eval);
            throw new ScriptException("Wrong result type: " + eval);
        }
    }

    public void destroy() {
        engine = null;
    }
}
