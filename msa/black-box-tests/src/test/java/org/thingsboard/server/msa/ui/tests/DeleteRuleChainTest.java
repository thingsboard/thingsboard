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
import org.thingsboard.server.msa.ui.utils.Const;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class DeleteRuleChainTest extends AbstractUiTest {

    @Test
    public void removeRuleChainByRightSideBtn() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        ruleChainsPage.createRuleChain(ENTITY_NAME);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainUI(ENTITY_NAME);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainIsNotPresent(deletedRuleChain));
    }

    @Test
    public void removeSelectedRuleChain() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChainName = ENTITY_NAME;
        ruleChainsPage.createRuleChain(ruleChainName);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainIsNotPresent(deletedRuleChain));
    }

    @Test
    public void removeFromRuleChainView() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        ruleChainsPage.createRuleChain(ENTITY_NAME);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.ruleChain(ENTITY_NAME).click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainFromView(ENTITY_NAME);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainIsNotPresent(deletedRuleChain));
    }

    @Test
    public void removeRootRuleChain() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertFalse(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME).isEnabled());
    }

    @Test
    public void removeSelectedRootRuleChain() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Test
    public void removeFromRootRuleChainView() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(Const.URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.ruleChain(ROOT_RULE_CHAIN_NAME).click();
        ruleChainsPage.deleteBtnFromView();

        ruleChainsPage.assertDeleteBtnInRootRuleChainIsNotDisplayed();
    }

    @Test
    public void removeRuleChainByRightSideBtnWithoutRefresh() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChainName = ENTITY_NAME;
        ruleChainsPage.createRuleChain(ruleChainName);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainUI(ruleChainName);

        Assert.assertTrue(ruleChainsPage.ruleChainIsNotPresent(deletedRuleChain));
    }
}
