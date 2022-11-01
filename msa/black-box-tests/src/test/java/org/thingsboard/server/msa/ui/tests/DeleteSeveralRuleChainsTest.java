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
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class DeleteSeveralRuleChainsTest extends AbstractUiTest {

    @Test
    public void canDeleteSeveralRuleChainsByTopBtn() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChainName = ENTITY_NAME;
        int count = 2;
        ruleChainsPage.createRuleChains(ruleChainName, count);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.clickOnCheckBoxes(count);

        ruleChainsPage.markCheckbox().forEach(x -> Assert.assertTrue(x.isDisplayed()));
        Assert.assertEquals(ruleChainsPage.markCheckbox().size(), count);

        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainIsNotPresent(ruleChainName));
    }

    @Test
    public void selectAllRuleChain() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChainName = ENTITY_NAME;
        int count = 2;
        ruleChainsPage.createRuleChains(ruleChainName, count);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        int ruleChainsCountBefore = ruleChainsPage.allEntity().size();
        ruleChainsPage.selectAllCheckBox().click();
        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();
        int ruleChainsCountAfter = ruleChainsPage.allEntity().size();

        Assert.assertTrue(ruleChainsCountBefore > ruleChainsCountAfter);
    }

    @Test
    public void removeRootRuleChain() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

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
        ruleChainsPage.selectAllCheckBox().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Test
    public void deleteSeveralRuleChainsByTopBtnWithoutRefresh() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChainName = ENTITY_NAME;
        int count = 2;
        ruleChainsPage.createRuleChains(ruleChainName, count);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.clickOnCheckBoxes(count);

        ruleChainsPage.markCheckbox().forEach(x -> Assert.assertTrue(x.isDisplayed()));
        Assert.assertEquals(ruleChainsPage.markCheckbox().size(), count);

        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainIsNotPresent(ruleChainName));
    }
}
