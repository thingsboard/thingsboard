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

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class SortByTimeTest extends AbstractDriverBaseTest {

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

    @Test(priority = 10, groups = "smoke")
    @Description
    public void sortByTimeDown() {
        String ruleChain = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain));
        ruleChainName = ruleChain;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(0).getText();

        Assert.assertEquals(firstListElement, lastCreated);
        Assert.assertNotNull(ruleChainsPage.createdTimeEntity(ruleChain, lastCreated));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void sortByTimeUp() {
        String ruleChain = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain));
        ruleChainName = ruleChain;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByTimeBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(ruleChainsPage.createdTime().size() - 1).getText();

        Assert.assertEquals(firstListElement, lastCreated);
        Assert.assertNotNull(ruleChainsPage.createdTimeEntity(ruleChain, lastCreated));
    }
}
