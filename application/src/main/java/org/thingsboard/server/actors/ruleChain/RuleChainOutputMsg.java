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
package org.thingsboard.server.actors.ruleChain;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.RuleNodeId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Created by ashvayka on 19.03.18.
 */
@EqualsAndHashCode(callSuper = true)
@ToString
public final class RuleChainOutputMsg extends TbToRuleChainActorMsg {

    @Getter
    private final RuleNodeId targetRuleNodeId;

    @Getter
    private final String relationType;

    public RuleChainOutputMsg(RuleChainId target, RuleNodeId targetRuleNodeId, String relationType, TbMsg tbMsg) {
        super(tbMsg, target);
        this.targetRuleNodeId = targetRuleNodeId;
        this.relationType = relationType;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.RULE_CHAIN_OUTPUT_MSG;
    }
}
