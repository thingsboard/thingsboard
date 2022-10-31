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

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class RuleChainEditMenuTest extends TestInit {

    @Test
    public void changeName() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        ruleChainsPage.createRuleChain(ENTITY_NAME);
        String name = "Changed";

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.setHeaderName();
        String nameBefore = ruleChainsPage.getHeaderName();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu(name);
        ruleChainsPage.doneBtnEditView().click();
        ruleChainsPage.setHeaderName();
        String nameAfter = ruleChainsPage.getHeaderName();

        Assert.assertNotEquals(nameBefore, nameAfter);
        Assert.assertEquals(name, nameAfter);

        ruleChainsPage.deleteRuleChain(name);
    }

    @Test
    public void deleteName() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu("");

        Assert.assertFalse(ruleChainsPage.doneBtnEditViewVisible().isEnabled());
    }

    @Test
    public void saveOnlyWithSpace() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu(" ");
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), "Rule chain name should be specified!");
    }

    @Test
    public void editDescription() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        ruleChainsPage.createRuleChain(ENTITY_NAME);
        String description = "Description";

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.description().sendKeys(description);
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertEquals(description, ruleChainsPage.description().getAttribute("value"));

        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.description().sendKeys(description);
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertEquals(description + description, ruleChainsPage.description().getAttribute("value"));

        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeDescription("");
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertTrue(ruleChainsPage.description().getAttribute("value").isEmpty());

        ruleChainsPage.deleteRuleChain(ENTITY_NAME);
    }

    @Test
    public void debugMode() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        ruleChainsPage.createRuleChain(ENTITY_NAME);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertTrue(Boolean.parseBoolean(ruleChainsPage.debugCheckboxView().getAttribute("aria-checked")));

        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();

        Assert.assertFalse(Boolean.parseBoolean(ruleChainsPage.debugCheckboxView().getAttribute("aria-checked")));

        ruleChainsPage.deleteRuleChain(ENTITY_NAME);
    }
}
