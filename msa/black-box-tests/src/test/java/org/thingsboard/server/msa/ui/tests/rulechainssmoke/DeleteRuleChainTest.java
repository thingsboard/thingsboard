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
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

@Feature("Delete rule chain")
public class DeleteRuleChainTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke")
    @Description("Remove the rule chain by clicking on the trash icon in the right side of rule chain")
    public void removeRuleChainByRightSideBtn() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove rule chain by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedRuleChain() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromRuleChainView() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ENTITY_NAME).click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainFromView(ruleChainName);
        jsClick(ruleChainsPage.refreshBtn());

        ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by clicking on the trash icon in the right side of rule chain")
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        assertIsDisable(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove root rule chain by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the root rule chain by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromRootRuleChainView() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ROOT_RULE_CHAIN_NAME).click();

        assertThat(ruleChainsPage.deleteBtnInRootRuleChainIsNotDisplayed())
                .as("Delete btn isn't displayed in details tab").isTrue();
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 10, groups = "smoke")
    @Description("Remove the rule chain with device profile by clicking on the trash icon in the right side of rule chain")
    public void removeProfileRuleChainByRightSideBtn() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteBtn(deletedRuleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(deletedRuleChain));
        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText())
                .as("Text of warning message").isEqualTo(DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain with device profile by mark in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedProfileRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected("Thermostat");
        ruleChainsPage.refreshBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(deletedRuleChain));
        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText())
                .as("Text of warning message").isEqualTo(DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Remove the rule chain with device profile by clicking on the 'Delete rule chain' btn in the entity view")
    public void removeFromProfileRuleChainView() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(deletedRuleChain).click();
        jsClick(ruleChainsPage.deleteBtnFromView());
        ruleChainsPage.warningPopUpYesBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(deletedRuleChain));
        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText())
                .as("Text of warning message").isEqualTo(DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Delete rule chain")
    @Test(priority = 30, groups = "smoke")
    @Description("Remove the rule chain by clicking on the trash icon in the right side of rule chain without refresh")
    public void removeRuleChainByRightSideBtnWithoutRefresh() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);

        ruleChainsPage.assertEntityIsNotPresent(deletedRuleChain);
    }
}
