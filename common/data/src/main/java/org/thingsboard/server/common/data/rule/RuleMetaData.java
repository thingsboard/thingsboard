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
package org.thingsboard.server.common.data.rule;

import lombok.Data;
import lombok.ToString;
import org.thingsboard.server.common.data.HasName;
import org.thingsboard.server.common.data.SearchTextBased;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.RuleId;
import org.thingsboard.server.common.data.id.TenantId;

import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.plugin.ComponentLifecycleState;

@Data
public class RuleMetaData extends SearchTextBased<RuleId> implements HasName {

    private static final long serialVersionUID = -5656679015122935465L;

    private TenantId tenantId;
    private String name;
    private ComponentLifecycleState state;
    private int weight;
    private String pluginToken;
    private JsonNode filters;
    private JsonNode processor;
    private JsonNode action;
    private JsonNode additionalInfo;

    public RuleMetaData() {
        super();
    }

    public RuleMetaData(RuleId id) {
        super(id);
    }

    public RuleMetaData(RuleMetaData rule) {
        super(rule);
        this.tenantId = rule.getTenantId();
        this.name = rule.getName();
        this.state = rule.getState();
        this.weight = rule.getWeight();
        this.pluginToken = rule.getPluginToken();
        this.filters = rule.getFilters();
        this.processor = rule.getProcessor();
        this.action = rule.getAction();
        this.additionalInfo = rule.getAdditionalInfo();
    }

    @Override
    public String getSearchText() {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }

}
