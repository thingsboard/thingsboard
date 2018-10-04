/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import lombok.Data;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.msg.MsgType;
import org.thingsboard.server.common.msg.aware.RuleChainAwareMsg;
import org.thingsboard.server.common.msg.aware.TenantAwareMsg;

import java.io.Serializable;

/**
 * Created by ashvayka on 19.03.18.
 */
@Data
final class RemoteToRuleChainTellNextMsg extends RuleNodeToRuleChainTellNextMsg implements TenantAwareMsg, RuleChainAwareMsg {

    private static final long serialVersionUID = 2459605482321657447L;
    private final TenantId tenantId;
    private final RuleChainId ruleChainId;

    public RemoteToRuleChainTellNextMsg(RuleNodeToRuleChainTellNextMsg original, TenantId tenantId, RuleChainId ruleChainId) {
        super(original.getOriginator(), original.getRelationTypes(), original.getMsg());
        this.tenantId = tenantId;
        this.ruleChainId = ruleChainId;
    }

    @Override
    public MsgType getMsgType() {
        return MsgType.REMOTE_TO_RULE_CHAIN_TELL_NEXT_MSG;
    }

}
