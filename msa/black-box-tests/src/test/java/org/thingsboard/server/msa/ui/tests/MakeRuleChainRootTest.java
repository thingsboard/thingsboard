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
import org.thingsboard.server.msa.ui.base.TestInit;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class MakeRuleChainRootTest extends TestInit {

    @Test
    public void makeRuleChainRootByRightCornerBtn() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getNotRootRuleChainName();
        ruleChainsPage.makeRootBtn(ruleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();

        Assert.assertTrue(ruleChainsPage.rootCheckBoxEnable(ruleChain).isDisplayed());

        ruleChainsPage.makeRoot();
    }

    @Test
    public void makeRuleChainRootFromView() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getNotRootRuleChainName();
        ruleChainsPage.ruleChain(ruleChain).click();
        ruleChainsPage.makeRootFromViewBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.closeEntityViewBtn().click();

        Assert.assertTrue(ruleChainsPage.rootCheckBoxEnable(ruleChain).isDisplayed());

        ruleChainsPage.makeRoot();
    }

    @Test
    public void multiplyRoot() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getNotRootRuleChainName();
        ruleChainsPage.ruleChain(ruleChain).click();
        ruleChainsPage.makeRootFromViewBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.closeEntityViewBtn().click();

        Assert.assertEquals(ruleChainsPage.rootCheckBoxesEnable().size(), 1);

        ruleChainsPage.makeRoot();
    }
}
