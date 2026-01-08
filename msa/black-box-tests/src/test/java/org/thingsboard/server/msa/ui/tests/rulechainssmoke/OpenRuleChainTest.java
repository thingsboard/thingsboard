/**
 * Copyright © 2016-2026 The Thingsboard Authors
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
package org.thingsboard.server.msa.ui.tests.rulechainssmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Open rule chain")
public class OpenRuleChainTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke")
    @Description("Open the rule chain by clicking on its name")
    public void openRuleChainByRightCornerBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        RuleChain ruleChain = getRuleChainByName(ruleChainName);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entity(ruleChainName).click();
        openRuleChainPage.setHeadName();

        assertThat(urlContains(ruleChain.getUuidId().toString())).as("URL contains rule chain's ID").isTrue();
        assertIsDisplayed(openRuleChainPage.headRuleChainName());
        assertIsDisplayed(openRuleChainPage.inputNode());
        assertThat(openRuleChainPage.getHeadName()).as("Head of opened rule chain page text").isEqualTo(ruleChainName);
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Open the rule chain by clicking on the 'Open rule chain' button in the entity view")
    public void openRuleChainByViewBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        RuleChain ruleChain = getRuleChainByName(ruleChainName);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.openRuleChainFromViewBtn().click();
        openRuleChainPage.setHeadName();

        assertThat(ruleChain).as("Rule chain created").isNotNull();
        assertThat(urlContains(ruleChain.getUuidId().toString())).as("URL contains rule chain's ID").isTrue();
        assertIsDisplayed(openRuleChainPage.headRuleChainName());
        assertIsDisplayed(openRuleChainPage.inputNode());
        assertThat(openRuleChainPage.getHeadName()).as("Head of opened rule chain page text").isEqualTo(ruleChainName);
    }
}
