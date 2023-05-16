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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.data.util.TbPair;

public interface VersionedNode {

    String VERSION_PROPERTY_NAME = "version";

    TbPair<Boolean, JsonNode> upgrade(RuleNodeId ruleNodeId, JsonNode oldConfiguration);

    default int getVersionOrElseThrowTbNodeException(RuleNodeId ruleNodeId, JsonNode oldConfiguration) throws TbNodeException {
        if (oldConfiguration == null) {
            throw new TbNodeException("Rule node: [" + this.getClass().getName() + "] " +
                    "with id: [" + ruleNodeId + "] has null configuration!");
        } else if (!oldConfiguration.isObject()) {
            throw new TbNodeException("Rule node: [" + this.getClass().getName() + "] " +
                    "with id: [" + ruleNodeId + "] has non json object configuration!");

        }
        if (!oldConfiguration.has(VERSION_PROPERTY_NAME)) {
            return 0;
        }
        return oldConfiguration.get(VERSION_PROPERTY_NAME).asInt();
    }

}
