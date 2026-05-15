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
package org.thingsboard.server.msa.security;

import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.AbstractContainerTest;

import static org.assertj.core.api.Assertions.assertThat;

public class JsExecutorSandboxIsolationTest extends AbstractContainerTest {

    @BeforeClass
    public void beforeClass() {
        testRestClient.login("tenant@thingsboard.org", "tenant");
    }

    @AfterClass
    public void afterClass() {
        testRestClient.resetToken();
    }

    /**
     * Black-box regression for JVN#16937365: a tenant admin must not be able
     * to escape the tb-js-executor sandbox via the host-realm prototype chain
     * exposed through the script's `args` argument. Runs against the live
     * docker-compose deployment, which uses script.use_sandbox=true and
     * JS_EVALUATOR=remote (Kafka -> tb-js-executor).
     */
    @Test
    public void testRuleChainScriptCannotReachHostProcess() {
        JsonNode response = testRestClient.testRuleChainScript("""
                {
                  "script": "var F = args.constructor.constructor; var p = F('return process')(); return { reachedHost: !!(p && p.mainModule) };",
                  "scriptType": "update",
                  "argNames": ["msg", "metadata", "msgType"],
                  "msg": "{}",
                  "metadata": {},
                  "msgType": "POST_TELEMETRY_REQUEST"
                }
                """);

        // The sandboxed run must reject the escape attempt: the host `process`
        // global is not defined inside the sandbox realm, so executing the
        // synthesized function `F("return process")` throws.
        assertThat(response.has("error")).isTrue();
        String error = response.get("error").asText();
        assertThat(error)
                .as("sandbox must block host-realm reach via args.constructor.constructor; full error: %s", error)
                .contains("process is not defined");

        // Defense in depth: even if the script somehow returned, output must
        // not indicate that the host process was reached.
        if (response.hasNonNull("output")) {
            assertThat(response.get("output").asText()).doesNotContain("\"reachedHost\":true");
        }
    }
}
