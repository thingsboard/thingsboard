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

import akka.actor.ActorContext;
import org.thingsboard.server.actors.ActorSystemContext;
import org.thingsboard.server.actors.service.DefaultActorService;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageDataIterable.FetchFunction;
import org.thingsboard.server.common.data.rule.RuleChain;

public class TenantRuleChainManager extends RuleChainManager {

    private final TenantId tenantId;

    public TenantRuleChainManager(ActorSystemContext systemContext, TenantId tenantId) {
        super(systemContext);
        this.tenantId = tenantId;
    }

    @Override
    public void init(ActorContext context) {
        super.init(context);
    }

    @Override
    protected TenantId getTenantId() {
        return tenantId;
    }

    @Override
    protected String getDispatcherName() {
        return DefaultActorService.TENANT_RULE_DISPATCHER_NAME;
    }

    @Override
    protected FetchFunction<RuleChain> getFetchEntitiesFunction() {
        return link -> service.findTenantRuleChains(tenantId, link);
    }
}
