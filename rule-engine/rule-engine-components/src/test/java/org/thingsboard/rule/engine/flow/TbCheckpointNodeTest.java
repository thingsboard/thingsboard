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
package org.thingsboard.rule.engine.flow;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.provider.Arguments;
import org.thingsboard.rule.engine.AbstractRuleNodeUpgradeTest;
import org.thingsboard.rule.engine.api.TbNode;

import java.util.stream.Stream;

import static org.mockito.Mockito.spy;

@Slf4j
public class TbCheckpointNodeTest extends AbstractRuleNodeUpgradeTest {

    // Rule nodes upgrade
    private static Stream<Arguments> givenFromVersionAndConfig_whenUpgrade_thenVerifyHasChangesAndConfig() {
        return Stream.of(
                // default config for version 0
                Arguments.of(0,
                        "{\"queueName\":null}",
                        true,
                        "{}"),
                // default config for version 0 with queueName
                Arguments.of(0,
                        "{\"queueName\":\"Main\"}",
                        true,
                        "{}"),
                // default config for version 1 with upgrade from version 0
                Arguments.of(0,
                        "{}",
                        false,
                        "{}")
        );
    }

    @Override
    protected TbNode getTestNode() {
        return spy(TbCheckpointNode.class);
    }
}
