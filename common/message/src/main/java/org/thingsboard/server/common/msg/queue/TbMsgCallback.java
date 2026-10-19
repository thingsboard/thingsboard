// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
