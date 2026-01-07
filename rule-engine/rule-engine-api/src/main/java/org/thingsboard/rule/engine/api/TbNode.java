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
package org.thingsboard.rule.engine.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.queue.PartitionChangeMsg;

import java.util.concurrent.ExecutionException;

/**
 * Created by ashvayka on 19.01.18.
 */
public interface TbNode {

    void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException;

    void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException;

    default void destroy() {
    }

    default void onPartitionChangeMsg(TbContext ctx, PartitionChangeMsg msg) {
    }

    /**
     * Upgrades the configuration from a specific version to the current version specified in the
     * {@link RuleNode} annotation for the instance of {@link TbNode}.
     *
     * @param fromVersion        The version from which the configuration needs to be upgraded.
     * @param oldConfiguration   The old configuration to be upgraded.
     * @return                   A pair consisting of a Boolean flag indicating the success of the upgrade
     *                           and a JsonNode representing the upgraded configuration.
     * @throws TbNodeException   If an error occurs during the upgrade process.
     */
    default TbPair<Boolean, JsonNode> upgrade(int fromVersion, JsonNode oldConfiguration) throws TbNodeException {
        return new TbPair<>(false, oldConfiguration);
    }

}
