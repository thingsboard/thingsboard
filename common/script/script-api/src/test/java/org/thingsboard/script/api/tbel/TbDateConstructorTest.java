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
package org.thingsboard.script.api.tbel;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mvel2.CompileException;
import org.mvel2.ExecutionContext;
import org.mvel2.ParserContext;
import org.mvel2.SandboxedParserConfiguration;

import java.io.Serializable;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mvel2.MVEL.compileExpression;
import static org.mvel2.MVEL.executeTbExpression;

public class TbDateConstructorTest {

    private static ExecutionContext executionContext;

    @BeforeAll
    public static void setup() {
        SandboxedParserConfiguration parserConfig = ParserContext.enableSandboxedMode();
        parserConfig.addImport("JSON", TbJson.class);
        parserConfig.registerDataType("Date", TbDate.class, date -> 8L);
        executionContext = new ExecutionContext(parserConfig, 5 * 1024 * 1024);
    }

    @AfterAll
    public static void tearDown() {
        ParserContext.disableSandboxedMode();
    }


    @Test
    void TestTbDateConstructorWithStringParameters () {
            // one: date in String
        String body = "var d = new Date(\"2023-08-06T04:04:05.123Z\"); \n" +
                "d.toISOString()";
        Object res = executeScript(body);
        Assertions.assertNotEquals("2023-08-06T04:04:05.123Z".length(),  res);

            // two: date in String + pattern
        body = "var pattern = \"yyyy-MM-dd HH:mm:ss.SSSXXX\";\n" +
                "var d = new Date(\"2023-08-06 04:04:05.000Z\", pattern);\n" +
                "d.toISOString()";
        res = executeScript(body);
        Assertions.assertNotEquals("2023-08-06T04:04:05Z".length(),  res);


        // three: date in String + pattern + locale
        body = "var pattern = \"hh:mm:ss a, EEE M/d/uuuu\";\n" +
                "var d = new Date(\"02:15:30 PM, Sun 10/09/2022\", pattern, \"en-US\");" +
                "d.toISOString()";
        res = executeScript(body);
        Assertions.assertNotEquals("2023-08-06T04:04:05Z".length(),  res);

        // four: date in String + pattern + locale + TimeZone
        body = "var pattern = \"hh:mm:ss a, EEE M/d/uuuu\";\n" +
                "var d = new Date(\"02:15:30 PM, Sun 10/09/2022\", pattern, \"en-US\", \"America/New_York\");" +
                "d.toISOString()";
        res = executeScript(body);
        Assertions.assertNotEquals("22022-10-09T18:15:30Z".length(),  res);
    }

    @Test
    void TbDateConstructorWithStringParameters_PatternNotMatchLocale_Error () {
        String expectedMessage = "could not create constructor: null";

        String body = "var pattern = \"hh:mm:ss a, EEE M/d/uuuu\";\n" +
                "var d = new Date(\"02:15:30 PM, Sun 10/09/2022\", pattern, \"de\");" +
                "d.toISOString()";
        Exception actual = assertThrows(CompileException.class, () -> {
            executeScript(body);
        });
        assertTrue(actual.getMessage().contains(expectedMessage));

    }

    private Object executeScript(String ex) {
        Serializable compiled = compileExpression(ex, new ParserContext());
        return executeTbExpression(compiled, executionContext,  new HashMap());
    }
}
