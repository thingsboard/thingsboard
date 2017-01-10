/**
 * Copyright © 2016-2017 The Thingsboard Authors
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
package org.thingsboard.server.actors.rule;

public class ComplexRuleActorChain implements RuleActorChain {

    private final RuleActorChain systemChain;
    private final RuleActorChain tenantChain;

    public ComplexRuleActorChain(RuleActorChain systemChain, RuleActorChain tenantChain) {
        super();
        this.systemChain = systemChain;
        this.tenantChain = tenantChain;
    }

    @Override
    public int size() {
        return systemChain.size() + tenantChain.size();
    }

    @Override
    public RuleActorMetaData getRuleActorMd(int index) {
        if (index < systemChain.size()) {
            return systemChain.getRuleActorMd(index);
        } else {
            return tenantChain.getRuleActorMd(index - systemChain.size());
        }
    }

}
