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
package org.thingsboard.rule.engine.mqtt;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbNode;

import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class TbMqttNodeTest extends AbstractRuleNodeUpgradeTest {
    @Spy
    TbMqttNode node;

    @BeforeEach
    public void setUp() throws Exception {
        node = mock(TbMqttNode.class);
    }

    @ParameterizedTest
    @ValueSource(strings = { "false", "\"", "\"\"", "\"This is a string with double quotes\"", "Path: /home/developer/test.txt",
            "First line\nSecond line\n\nFourth line", "Before\rAfter", "Tab\tSeparated\tValues", "Test\bbackspace", "[]",
            "[1, 2, 3]", "{\"key\": \"value\"}", "{\n\"temperature\": 25.5,\n\"humidity\": 50.2\n\"}", "Expression: (a + b) * c",
            "世界", "Україна", "\u1F1FA\u1F1E6", "🇺🇦"})
    public void testParseJsonStringToPlainText(String original) {
        Mockito.when(node.parseJsonStringToPlainText(anyString())).thenCallRealMethod();

        String serialized = JacksonUtil.toString(original);
        Assertions.assertNotNull(serialized);
        Assertions.assertEquals(original, node.parseJsonStringToPlainText(serialized));
    }

    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"}}",
                        true,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(1,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false}",
                        false,
                        "{\"topicPattern\":\"my-topic\",\"port\":1883,\"connectTimeoutSec\":10,\"cleanSession\":true, \"ssl\":false, \"retainedMessage\":false,\"credentials\":{\"type\":\"anonymous\"},\"parseToPlainText\":false}")
        );

    }

    @Override
    protected TbNode getTestNode() {
        return node;
    }
}
