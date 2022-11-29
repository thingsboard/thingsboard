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

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_RULE_CHAIN_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;
import static org.thingsboard.server.msa.ui.utils.Const.URL;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

public class RuleChainEditMenuTest extends AbstractDriverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelper ruleChainsPage;
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        openUrl(URL);
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
    public void changeName() {
        testRestClient.postRuleChain(defaultRuleChainPrototype(ENTITY_NAME));
        String name = "Changed";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.setHeaderName();
        String nameBefore = ruleChainsPage.getHeaderName();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu(name);
        ruleChainsPage.doneBtnEditView().click();
        ruleChainsPage.setHeaderName();
        String nameAfter = ruleChainsPage.getHeaderName();
        ruleChainName = name;

        Assert.assertNotEquals(nameBefore, nameAfter);
        Assert.assertEquals(name, nameAfter);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void deleteName() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu("");

        Assert.assertFalse(ruleChainsPage.doneBtnEditViewVisible().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void saveOnlyWithSpace() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu(" ");
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), EMPTY_RULE_CHAIN_MESSAGE);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void editDescription() {
        ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));
        String description = "Description";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.descriptionEntityView().sendKeys(description);
        ruleChainsPage.doneBtnEditView().click();
        String description1 = ruleChainsPage.descriptionEntityView().getAttribute("value");
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.descriptionEntityView().sendKeys(description);
        ruleChainsPage.doneBtnEditView().click();
        String description2 = ruleChainsPage.descriptionEntityView().getAttribute("value");
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeDescription("");
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertTrue(ruleChainsPage.descriptionEntityView().getAttribute("value").isEmpty());
        Assert.assertEquals(description, description1);
        Assert.assertEquals(description + description, description2);
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void debugMode() {
        ruleChainName = ENTITY_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();
        boolean debugMode = Boolean.parseBoolean(ruleChainsPage.debugCheckboxView().getAttribute("aria-checked"));
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertFalse(Boolean.parseBoolean(ruleChainsPage.debugCheckboxView().getAttribute("aria-checked")));
        Assert.assertTrue(debugMode);
    }
}