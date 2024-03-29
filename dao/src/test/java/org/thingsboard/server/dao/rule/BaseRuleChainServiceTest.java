/**
 * Copyright © 2016-2024 The Thingsboard Authors
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
package org.thingsboard.server.dao.rule;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

@DaoSqlTest
public class BaseRuleChainServiceTest extends AbstractServiceTest {

    @Autowired
    private BaseRuleChainService ruleChainService;

    @Test
    public void givenRuleChain_whenSave_thenReturnsSavedRuleChain() {
        RuleChain ruleChain = getRuleChain();
        ruleChain.setTenantId(tenantId);
        RuleChain savedRuleChain = ruleChainService.saveRuleChain(ruleChain);

        Assertions.assertThat(savedRuleChain).isNotNull();
        Assertions.assertThat(savedRuleChain.getId()).isNotNull();
        Assertions.assertThat(savedRuleChain.getCreatedTime() > 0).isTrue();
        Assertions.assertThat(ruleChain.getTenantId()).isEqualTo(savedRuleChain.getTenantId());

        RuleChain foundRuleChain = ruleChainService.findRuleChainById(tenantId, savedRuleChain.getId());
        Assertions.assertThat(savedRuleChain.getName()).isEqualTo(foundRuleChain.getName());

        ruleChainService.deleteRuleChainsByTenantId(tenantId);
    }

    @Test
    public void givenRuleChainWithExistingExternalId_whenSave_thenThrowsException() {
        RuleChainId externalRuleChainId = new RuleChainId(UUID.fromString("2675d180-e1e5-11ee-9f06-71b6c7dc2cbf"));

        RuleChain ruleChain = getRuleChain();
        ruleChain.setTenantId(tenantId);
        ruleChain.setExternalId(externalRuleChainId);
        ruleChainService.saveRuleChain(ruleChain);

        Assertions.assertThatThrownBy(() -> ruleChainService.saveRuleChain(ruleChain))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Rule Chain with such external id already exists!");

        ruleChainService.deleteRuleChainsByTenantId(tenantId);
    }

    private RuleChain getRuleChain() {
        String ruleChainStr = "{\n" +
                "  \"name\": \"Root Rule Chain\",\n" +
                "  \"type\": \"CORE\",\n" +
                "  \"firstRuleNodeId\": {\n" +
                "    \"entityType\": \"RULE_NODE\",\n" +
                "    \"id\": \"91ad0b00-e779-11ee-9cf0-15d8b6079fdb\"\n" +
                "  },\n" +
                "  \"debugMode\": false,\n" +
                "  \"configuration\": null,\n" +
                "  \"additionalInfo\": null\n" +
                "}";
        return JacksonUtil.fromString(ruleChainStr, RuleChain.class);
    }

}
