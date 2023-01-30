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
import org.thingsboard.server.msa.ui.pages.OpenRuleChainPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class OpenRuleChainTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;
    private OpenRuleChainPageHelper openRuleChainPage;
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
        openRuleChainPage = new OpenRuleChainPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (ruleChainName != null) {
            deleteRuleChain(ruleChainName);
            ruleChainName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void openRuleChainByRightCornerBtn() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entity(ruleChainName).click();
        openRuleChainPage.setHeadName();

        Assert.assertTrue(urlContains(String.valueOf(getRuleChainByName(ruleChainName).getId())));
        Assert.assertTrue(openRuleChainPage.headRuleChainName().isDisplayed());
        Assert.assertTrue(openRuleChainPage.inputNode().isDisplayed());
        Assert.assertEquals(ruleChainName, openRuleChainPage.getHeadName());
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void openRuleChainByViewBtn() {
        String ruleChainName = ENTITY_NAME + random();
        testRestClient.postRuleChain(EntityPrototypes.defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.detailsBtn(ruleChainName).click();
        ruleChainsPage.openRuleChainFromViewBtn().click();
        openRuleChainPage.setHeadName();

        Assert.assertTrue(urlContains(String.valueOf(getRuleChainByName(ruleChainName).getId())));
        Assert.assertTrue(openRuleChainPage.headRuleChainName().isDisplayed());
        Assert.assertTrue(openRuleChainPage.inputNode().isDisplayed());
        Assert.assertEquals(ruleChainName, openRuleChainPage.getHeadName());
    }
}
