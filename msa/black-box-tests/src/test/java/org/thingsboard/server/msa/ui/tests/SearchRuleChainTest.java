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
import static org.thingsboard.server.msa.ui.utils.TestUtil.randomSymbol;

public class SearchRuleChainTest extends AbstractUiTest {

    @Test
    public void searchFirstWord() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String namePath = "Root";

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchRuleChain(namePath);
        ruleChainsPage.setRuleChainName(0);

        Assert.assertTrue(ruleChainsPage.getNotRootRuleChainName().contains(namePath));
    }

    @Test
    public void searchSecondWord() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String namePath = "Rule";

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchRuleChain(namePath);
        ruleChainsPage.setRuleChainName(0);

        Assert.assertTrue(ruleChainsPage.getNotRootRuleChainName().contains(namePath));
    }

    @Test
    public void searchNumber() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChain = ENTITY_NAME;
        ruleChainsPage.createRuleChain(ruleChain);
        String namePath = ruleChain.split("`")[1];

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchRuleChain(namePath);
        ruleChainsPage.setRuleChainName(0);

        Assert.assertTrue(ruleChainsPage.getNotRootRuleChainName().contains(namePath));

        ruleChainsPage.deleteRuleChain(ruleChain);
    }

    @Test
    public void searchSymbols() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        String ruleChain = ENTITY_NAME;
        ruleChainsPage.createRuleChain(ruleChain);
        String namePath = String.valueOf(randomSymbol());

        System.out.println(namePath);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchRuleChain(namePath);
        ruleChainsPage.setRuleChainName(0);

        Assert.assertTrue(ruleChainsPage.getNotRootRuleChainName().contains(namePath));

        ruleChainsPage.deleteRuleChain(ruleChain);
    }
}
