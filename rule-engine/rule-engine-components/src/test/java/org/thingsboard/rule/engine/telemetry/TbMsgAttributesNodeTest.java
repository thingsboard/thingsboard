/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.rule.engine.telemetry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.server.common.data.util.TbPair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.willCallRealMethod;
import static org.mockito.Mockito.mock;

@Slf4j
class TbMsgAttributesNodeTest {

    @Test
    void testUpgrade_fromVersion0() throws TbNodeException {
        final String updateAttributesOnValueChangeKey = "updateAttributesOnValueChange";
        TbMsgAttributesNode node = mock(TbMsgAttributesNode.class);
        willCallRealMethod().given(node).upgrade(anyInt(), any());

        ObjectNode jsonNode = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        jsonNode.remove(updateAttributesOnValueChangeKey);
        assertThat(jsonNode.has(updateAttributesOnValueChangeKey)).as("pre condition has no " + updateAttributesOnValueChangeKey).isFalse();

        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, jsonNode);

        ObjectNode resultNode = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradeResult.getFirst()).as("upgrade result has changes").isTrue();
        assertThat(resultNode.has(updateAttributesOnValueChangeKey)).as("upgrade result has key " + updateAttributesOnValueChangeKey).isTrue();
        assertThat(resultNode.get(updateAttributesOnValueChangeKey).asBoolean()).as("upgrade result value [false] for key " + updateAttributesOnValueChangeKey).isFalse();
    }

    @Test
    void testUpgrade_fromVersion0_alreadyHasUpdateAttributesOnValueChange() throws TbNodeException {
        final String updateAttributesOnValueChangeKey = "updateAttributesOnValueChange";
        TbMsgAttributesNode node = mock(TbMsgAttributesNode.class);
        willCallRealMethod().given(node).upgrade(anyInt(), any());

        ObjectNode jsonNode = (ObjectNode) JacksonUtil.valueToTree(new TbMsgAttributesNodeConfiguration().defaultConfiguration());
        jsonNode.remove(updateAttributesOnValueChangeKey);
        jsonNode.put(updateAttributesOnValueChangeKey, true);
        assertThat(jsonNode.has(updateAttributesOnValueChangeKey)).as("pre condition has no " + updateAttributesOnValueChangeKey).isTrue();
        assertThat(jsonNode.get(updateAttributesOnValueChangeKey).asBoolean()).as("pre condition has [true] for key " + updateAttributesOnValueChangeKey).isTrue();

        TbPair<Boolean, JsonNode> upgradeResult = node.upgrade(0, jsonNode);

        ObjectNode resultNode = (ObjectNode) upgradeResult.getSecond();
        assertThat(upgradeResult.getFirst()).as("upgrade result has changes").isFalse();
        assertThat(resultNode.has(updateAttributesOnValueChangeKey)).as("upgrade result has key " + updateAttributesOnValueChangeKey).isTrue();
        assertThat(resultNode.get(updateAttributesOnValueChangeKey).asBoolean()).as("upgrade result value [true] for key " + updateAttributesOnValueChangeKey).isTrue();
    }

}
