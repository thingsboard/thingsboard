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

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

@Feature("Delete several rule chains")
public class DeleteSeveralRuleChainsTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke")
    @Description("Remove several rule chains by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top")
    public void canDeleteSeveralRuleChainsByTopBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteSelected(2);
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName);
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Remove several rule chains by mark all the rule chains on the page by clicking in the topmost checkbox" +
            " and then clicking on the trash icon in the menu that appears")
    public void selectAllRuleChain() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();
        jsClick(ruleChainsPage.deleteSelectedBtn());
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top")
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        assertIsDisable(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by mark all the rule chains on the page by clicking in the topmost checkbox" +
            " and then clicking on the trash icon in the menu that appears")
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Test(priority = 30, groups = "smoke")
    @Description("Remove several rule chains by mark in the checkbox and then click on the trash can icon in the menu " +
            "that appears at the top without refresh")
    public void deleteSeveralRuleChainsByTopBtnWithoutRefresh() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteSelected(2);

        ruleChainsPage.assertRuleChainsIsNotPresent(ruleChainName);
    }
}
