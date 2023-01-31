/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Epics;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import java.util.ArrayList;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_RULE_CHAIN_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class CreateRuleChainTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (ruleChainName != null) {
            testRestClient.deleteRuleChain(getRuleChainByName(ruleChainName).getId());
            ruleChainName = null;
        }
    }

    @Epic("Rule chains smoke tests")
    @Feature("Create rule chain")
    @Test(priority = 10, groups = "smoke")
    @Description("Add rule chain after specifying the name (text/numbers /special characters)")
    public void createRuleChain() {
        String ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().click();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();
        this.ruleChainName = ruleChainName;

        Assert.assertNotNull(ruleChainsPage.entity(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());
    }

    @Epic("Rule chains smoke tests")
    @Feature("Create rule chain")
    @Test(priority = 10, groups = "smoke")
    @Description("Add rule chain after specifying the name and description (text/numbers /special characters)")
    public void createRuleChainWithDescription() {
        String ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.descriptionAddEntityView().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();
        this.ruleChainName = ruleChainName;
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.setHeaderName();

        Assert.assertEquals(ruleChainsPage.getHeaderName(), ruleChainName);
        Assert.assertEquals(ruleChainsPage.descriptionEntityView().getAttribute("value"), ruleChainName);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Create rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Add rule chain without the name")
    public void createRuleChainWithoutName() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();

        Assert.assertFalse(ruleChainsPage.addBtnV().isEnabled());
    }

    @Epic("Rule chains smoke tests")
    @Feature("Create rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Create rule chain only with spase in name")
    public void createRuleChainWithOnlySpace() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(" ");
        ruleChainsPage.addBtnC().click();

        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), EMPTY_RULE_CHAIN_MESSAGE);
        Assert.assertNotNull(ruleChainsPage.addEntityView());
        Assert.assertTrue(ruleChainsPage.addEntityView().isDisplayed());
    }

    @Epic("Rule chains smoke tests")
    @Feature("Create rule chain")
    @Test(priority = 20, groups = "smoke")
    @Description("Create a rule chain with the same name")
    public void createRuleChainWithSameName() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();

        boolean entityNotNull = ruleChainsPage.entity(ruleChainName) != null;
        boolean entitiesSizeMoreOne = ruleChainsPage.entities(ruleChainName).size() > 1;
        ArrayList<Boolean> entityIsDisplayed = new ArrayList<>();
        ruleChainsPage.entities(ruleChainName).forEach(x -> entityIsDisplayed.add(x.isDisplayed()));

        testRestClient.deleteRuleChain(getRuleChainByName(ruleChainName).getId());

        Assert.assertTrue(entityNotNull);
        Assert.assertTrue(entitiesSizeMoreOne);
        entityIsDisplayed.forEach(Assert::assertTrue);
    }

    @Epic("Rule chains smoke tests")
    @Feature("Create rule chain")
    @Test(priority = 30, groups = "smoke")
    @Description("Add rule chain after specifying the name (text/numbers /special characters) without refresh")
    public void createRuleChainWithoutRefresh() {
        String ruleChainName = ENTITY_NAME + random();

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        this.ruleChainName = ruleChainName;

        Assert.assertNotNull(ruleChainsPage.entity(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());
    }

    @Epic("Rule chains smoke tests")
    @Feature("Add rule chain after specifying the name (text/numbers /special characters) without refresh")
    @Test(priority = 40, groups = "smoke")
    @Description("Go to rule chain documentation page")
    public void documentation() {
        String urlPath = "docs/user-guide/ui/rule-chains/";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot();
        ruleChainsPage.detailsBtn(ruleChainsPage.getRuleChainName()).click();
        ruleChainsPage.goToHelpPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}
