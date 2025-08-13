/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.msg.queue;

import org.thingsboard.server.common.data.id.RuleNodeId;

/**
 * Should be renamed to TbMsgPackContext, but this can't be changed due to backward-compatibility.
 */
public interface TbMsgCallback {

    TbMsgCallback EMPTY = new TbMsgCallback() {

        @Override
        public void onSuccess() {

        }

        @Override
        public void onFailure(RuleEngineException e) {

        }
    };

    void onSuccess();

    void onFailure(RuleEngineException e);

    default void onRateLimit(RuleEngineException e) {
        onFailure(e);
    };

    /**
     * Returns 'true' if rule engine is expecting the message to be processed, 'false' otherwise.
     * message may no longer be valid, if the message pack is already expired/canceled/failed.
     *
     * @return 'true' if rule engine is expecting the message to be processed, 'false' otherwise.
     */
    default boolean isMsgValid() {
        return true;
    }

    default void onProcessingStart(RuleNodeInfo ruleNodeInfo) {
    }

    default void onProcessingEnd(RuleNodeId ruleNodeId) {
    }

}
