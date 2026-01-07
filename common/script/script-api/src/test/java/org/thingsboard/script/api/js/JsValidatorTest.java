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
package org.thingsboard.script.api.js;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class JsValidatorTest {

    @ParameterizedTest(name = "should return error for script \"{0}\"")
    @ValueSource(strings = {
            "async function test() {}",
            "const result = await someFunc();",
            "const result =\nawait\tsomeFunc();",
            "setTimeout(1000);",
            "new Promise((resolve) => {});",
            "function test() { return 42; } \n\t await test()",
            """
                function init() {
                  await doSomething();
                }
            """,
    })
    void shouldReturnErrorForInvalidScripts(String script) {
        assertNotNull(JsValidator.validate(script));
    }

    @ParameterizedTest(name = "should pass validation for script: \"{0}\"")
    @ValueSource(strings = {
            "function test() { return 42; }",
            "const result = 10 * 2;",
            "// async is a keyword but not used: 'const word = \"async\";'",
            "let note = 'setTimeout tight';",

            "const word = \"async\";",
            "const word = \"setTimeout\";",
            "const word = \"Promise\";",
            "const word = \"await\";",

            "const word = 'async';",
            "const word = 'setTimeout';",
            "const word = 'Promise';",
            "const word = 'await';",

            "//function test() { return 42; }",
            "// const result = 10 * 2;",
            "// async is a keyword but not used: 'const word = \"async\";'",
            "//setTimeout(1);",

            "a=b+c; // await for a day",
            "return new // Promise((resolve) => {",
            "hello(); // async is a keyword but not used: 'const word = \"async\";'",
            "setGoal(a); //setTimeout(1);",

            " /* new Promise((resolve) => {}); // */ return 'await';",
            " /* async */ function calc() {",
            "/* async function abc() { \n await new Promise ( \t setTimeout () ) \n } \n*/",
    })
    void shouldReturnNullForValidScripts(String script) {
        assertNull(JsValidator.validate(script));
    }

    @ParameterizedTest(name = "should return 'Script body is empty' for input: \"{0}\"")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void shouldReturnErrorForEmptyOrNullScripts(String script) {
        assertEquals("Script body is empty", JsValidator.validate(script));
    }

}
