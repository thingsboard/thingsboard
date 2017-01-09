/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.extensions.core.filter;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.*;

/**
 * @author Andrew Shvayka
 */
@Slf4j
public class NashornJsEvaluator {

    private static NashornScriptEngineFactory factory = new NashornScriptEngineFactory();

    private CompiledScript engine;

    public NashornJsEvaluator(String script) {
        engine = compileScript(script);
    }

    private static CompiledScript compileScript(String script) {
        ScriptEngine engine = factory.getScriptEngine(new String[]{"--no-java"});
        Compilable compEngine = (Compilable) engine;
        try {
            return compEngine.compile(script);
        } catch (ScriptException e) {
            log.warn("Failed to compile filter script: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Can't compile script: " + e.getMessage());
        }
    }

    public Boolean execute(Bindings bindings) throws ScriptException {
        Object eval = engine.eval(bindings);
        if (eval instanceof Boolean) {
            return (Boolean) eval;
        } else {
            log.warn("Wrong result type: {}", eval);
            throw new ScriptException("Wrong result type: " + eval);
        }
    }

    public void destroy() {
        engine = null;
    }
}
