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
package org.thingsboard.server.msa.ui.tests;

import org.junit.Assert;
import org.junit.Test;
import org.thingsboard.server.msa.ui.base.AbstractUiTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class CreateRuleChainTest extends AbstractUiTest {

    @Test
    public void createRuleChain() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChainName = ENTITY_NAME;

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChain(ruleChainName).isDisplayed());

        ruleChainsPage.deleteRuleChain(ruleChainName);
    }

    @Test
    public void createRuleChainWithoutName() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();

        Assert.assertFalse(ruleChainsPage.addBtnV().isEnabled());
    }

    @Test
    public void createRuleChainWithSameName() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot();
        ruleChainsPage.openCreateRuleChainView();
        String ruleChainName = ruleChainsPage.getNotRootRuleChainName();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entities(ruleChainName).size() > 1);

        ruleChainsPage.deleteRuleChain(ruleChainName);
    }

    @Test
    public void createRuleChainWithoutRefresh() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChainName = ENTITY_NAME;

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();

        Assert.assertTrue(ruleChainsPage.ruleChain(ruleChainName).isDisplayed());

        ruleChainsPage.deleteRuleChain(ruleChainName);
    }

    @Test
    public void documentation() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String urlPath = "docs/user-guide/ui/rule-chains/";

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot();
        ruleChainsPage.ruleChain(ruleChainsPage.getNotRootRuleChainName()).click();
        ruleChainsPage.goToNextTab();

        Assert.assertTrue(urlContains(urlPath));
    }
}
