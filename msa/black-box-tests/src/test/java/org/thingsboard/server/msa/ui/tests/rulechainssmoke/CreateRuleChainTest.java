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
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_RULE_CHAIN_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

@Feature("Create rule chain")
public class CreateRuleChainTest extends AbstractRuleChainTest {

    @Test(priority = 10, groups = "smoke")
    @Description("Add rule chain after specifying the name (text/numbers /special characters)")
    public void createRuleChain() {
        ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().click();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(ruleChainName));
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Add rule chain after specifying the name and description (text/numbers /special characters)")
    public void createRuleChainWithDescription() {
        ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.descriptionAddEntityView().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.setHeaderName();

        assertThat(ruleChainsPage.getHeaderName()).as("Header of rule chain details tab").isEqualTo(ruleChainName);
        assertThat(ruleChainsPage.descriptionEntityView().getAttribute("value"))
                .as("Description in rule chain details tab").isEqualTo(ruleChainName);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Add rule chain without the name")
    public void createRuleChainWithoutName() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();

        assertIsDisable(ruleChainsPage.addBtnV());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Create rule chain only with spase in name")
    public void createRuleChainWithOnlySpace() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(" ");
        ruleChainsPage.addBtnC().click();

        assertIsDisplayed(ruleChainsPage.warningMessage());
        assertThat(ruleChainsPage.warningMessage().getText()).as("Text of warning message").isEqualTo(EMPTY_RULE_CHAIN_MESSAGE);
        assertIsDisplayed(ruleChainsPage.addEntityView());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Create a rule chain with the same name")
    public void createRuleChainWithSameName() {
        ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();

        assertThat(ruleChainsPage.entities(ruleChainName).size() > 1).
                as("More than 1 rule chains have been created").isTrue();
        ruleChainsPage.entities(ruleChainName).forEach(this::assertIsDisplayed);
    }

    @Test(priority = 30, groups = "smoke")
    @Description("Add rule chain after specifying the name (text/numbers /special characters) without refresh")
    public void createRuleChainWithoutRefresh() {
        ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();

        assertIsDisplayed(ruleChainsPage.entity(ruleChainName));
    }

    @Test(priority = 40, groups = "smoke")
    @Description("Go to rule chain documentation page")
    public void documentation() {
        String urlPath = "docs/user-guide/ui/rule-chains/";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot();
        ruleChainsPage.detailsBtn(ruleChainsPage.getRuleChainName()).click();
        ruleChainsPage.goToHelpPage();

        assertThat(urlContains(urlPath)).as("Redirected URL contains " + urlPath).isTrue();
    }
}
