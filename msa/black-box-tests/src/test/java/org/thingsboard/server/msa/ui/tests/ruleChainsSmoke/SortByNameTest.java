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
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class SortByNameTest extends AbstractDriverBaseTest {

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
            if (getRuleChainByName(ruleChainName) != null) {
                testRestClient.deleteRuleChain(getRuleChainByName(ruleChainName).getId());
            }
            ruleChainName = null;
        }
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterUp(String ruleChainName) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameBtn().click();
        ruleChainsPage.setRuleChainName(0);

        Assert.assertEquals(ruleChainsPage.getRuleChainName(), ruleChainName);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortUp(String ruleChain, String ruleChainSymbol, String ruleChainNumber) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainSymbol));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainNumber));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameBtn().click();
        ruleChainsPage.setRuleChainName(0);
        String firstRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(1);
        String secondRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(2);
        String thirdRuleChain = ruleChainsPage.getRuleChainName();

        testRestClient.deleteRuleChain(getRuleChainByName(ruleChain).getId());
        testRestClient.deleteRuleChain(getRuleChainByName(ruleChainNumber).getId());
        testRestClient.deleteRuleChain(getRuleChainByName(ruleChainSymbol).getId());

        Assert.assertEquals(firstRuleChain, ruleChainSymbol);
        Assert.assertEquals(secondRuleChain, ruleChainNumber);
        Assert.assertEquals(thirdRuleChain, ruleChain);
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterDown(String ruleChainName) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        this.ruleChainName = ruleChainName;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameDown();
        ruleChainsPage.setRuleChainName(ruleChainsPage.allNames().size() - 1);

        Assert.assertEquals(ruleChainsPage.getRuleChainName(), ruleChainName);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortDown(String ruleChain, String ruleChainSymbol, String ruleChainNumber) {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainSymbol));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChain));
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainNumber));

        sideBarMenuView.ruleChainsBtn().click();
        int lastIndex = ruleChainsPage.allNames().size() - 1;
        ruleChainsPage.sortByNameDown();
        ruleChainsPage.setRuleChainName(lastIndex);
        String firstRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(lastIndex - 1);
        String secondRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(lastIndex - 2);
        String thirdRuleChain = ruleChainsPage.getRuleChainName();

        testRestClient.deleteRuleChain(getRuleChainByName(ruleChain).getId());
        testRestClient.deleteRuleChain(getRuleChainByName(ruleChainNumber).getId());
        testRestClient.deleteRuleChain(getRuleChainByName(ruleChainSymbol).getId());

        Assert.assertEquals(firstRuleChain, ruleChainSymbol);
        Assert.assertEquals(secondRuleChain, ruleChainNumber);
        Assert.assertEquals(thirdRuleChain, ruleChain);
    }
}