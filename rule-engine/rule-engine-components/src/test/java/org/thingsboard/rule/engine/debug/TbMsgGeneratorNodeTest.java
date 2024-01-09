/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.rule.engine.debug;

import org.junit.jupiter.params.provider.Arguments;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbNode;

import java.util.stream.Stream;

import static org.mockito.Mockito.spy;

public class TbMsgGeneratorNodeTest extends AbstractRuleNodeUpgradeTest {

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"queueName\":null, \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        true,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}"),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"queueName\":\"Main\", \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        true,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}",
                        false,
                        "{\"msgCount\":0,\"periodInSeconds\":1,\"originatorId\":null,\"originatorType\":null, \"scriptLang\":\"TBEL\",\"jsScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\",\"tbelScript\":\"var msg = { temp: 42, humidity: 77 };\\nvar metadata = { data: 40 };\\nvar msgType = \\\"POST_TELEMETRY_REQUEST\\\";\\n\\nreturn { msg: msg, metadata: metadata, msgType: msgType };\"}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return spy(TbMsgGeneratorNode.class);
    }
}
