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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.ROOT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;
import static org.thingsboard.server.msa.ui.utils.Const.URL;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class DeleteSeveralRuleChainsTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelper(driver).authorizationTenant();
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void canDeleteSeveralRuleChainsByTopBtn() {
        String ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.clickOnCheckBoxes(2);
        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainsIsNotPresent(ruleChainName));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void selectAllRuleChain() {
        String ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();
        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainsIsNotPresent(ruleChainName));
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        Assert.assertFalse(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME).isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Test(priority = 30, groups = "smoke")
    @Description
    public void deleteSeveralRuleChainsByTopBtnWithoutRefresh() {
        String ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName + 1));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.clickOnCheckBoxes(2);
        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainsIsNotPresent(ruleChainName));
    }
}
