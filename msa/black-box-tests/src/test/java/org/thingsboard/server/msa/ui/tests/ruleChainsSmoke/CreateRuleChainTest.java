/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_RULE_CHAIN_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

public class CreateRuleChainTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        openLocalhost();
        new LoginPageHelper(driver).authorizationTenant();
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
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

    @Test(priority = 10, groups = "smoke")
    @Description
    public void createRuleChain() {
        String ruleChainName = ENTITY_NAME;

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

    @Test(priority = 10, groups = "smoke")
    @Description
    public void createRuleChainWithDescription() {
        String ruleChainName = ENTITY_NAME;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.descriptionAddEntityView().sendKeys(ENTITY_NAME);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();
        this.ruleChainName = ruleChainName;
        ruleChainsPage.detailsBtn(ENTITY_NAME).click();
        ruleChainsPage.setHeaderName();

        Assert.assertEquals(ruleChainsPage.getHeaderName(), ruleChainName);
        Assert.assertEquals(ruleChainsPage.descriptionEntityView().getAttribute("value"), ruleChainName);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createRuleChainWithoutName() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();

        Assert.assertFalse(ruleChainsPage.addBtnV().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
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

    @Test(priority = 20, groups = "smoke")
    @Description
    public void createRuleChainWithSameName() {
        String ruleChainName = ENTITY_NAME;
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

    @Test(priority = 30, groups = "smoke")
    @Description
    public void createRuleChainWithoutRefresh() {
        String ruleChainName = ENTITY_NAME;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        this.ruleChainName = ruleChainName;

        Assert.assertNotNull(ruleChainsPage.entity(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());
    }

    @Test(priority = 40, groups = "smoke")
    @Description
    public void documentation() {
        String urlPath = "docs/user-guide/ui/rule-chains/";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot();
        ruleChainsPage.detailsBtn(ruleChainsPage.getRuleChainName()).click();
        ruleChainsPage.goToHelpPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}
