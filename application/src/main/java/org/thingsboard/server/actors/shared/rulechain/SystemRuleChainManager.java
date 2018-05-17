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
package org.thingsboard.server.actors.shared.rulechain;

import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable.FetchFunction;
import org.thingsboard.server.common.data.page.TextPageData;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Collections;

public class SystemRuleChainManager extends RuleChainManager {

    public SystemRuleChainManager(ActorSystemContext systemContext) {
        super(systemContext);
    }

    @Override
    protected FetchFunction<RuleChain> getFetchEntitiesFunction() {
        return link -> new TextPageData<>(Collections.emptyList(), link);
    }

    @Override
    protected TenantId getTenantId() {
        return ModelConstants.SYSTEM_TENANT;
    }

    @Override
    protected String getDispatcherName() {
        return DefaultActorService.SYSTEM_RULE_DISPATCHER_NAME;
    }
}
