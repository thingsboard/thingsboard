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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SimpleRuleActorChain implements RuleActorChain {

    private final List<RuleActorMetaData> rules;

    public SimpleRuleActorChain(Set<RuleActorMetaData> ruleSet) {
        rules = new ArrayList<>(ruleSet);
        rules.sort(RuleActorMetaData.RULE_ACTOR_MD_COMPARATOR);
    }

    public int size() {
        return rules.size();
    }

    public RuleActorMetaData getRuleActorMd(int index) {
        return rules.get(index);
    }

}
