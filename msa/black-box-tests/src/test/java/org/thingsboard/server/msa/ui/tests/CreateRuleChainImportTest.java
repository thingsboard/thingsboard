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
import org.junit.Before;
import org.junit.Test;
import org.thingsboard.server.msa.ui.base.TestInit;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.OpenRuleChainPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_IMPORT_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_TXT_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class CreateRuleChainImportTest extends TestInit {

    private String absolutePathToFileImportRuleChain;
    private String absolutePathToFileImportTxt;

    @Before
    public void init() {
        absolutePathToFileImportRuleChain = getClass().getClassLoader().getResource(IMPORT_RULE_CHAIN_FILE_NAME).getPath();
        absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();
    }

    @Test
    public void importRuleChain() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);

        Assert.assertTrue(ruleChainsPage.importingFile(IMPORT_RULE_CHAIN_FILE_NAME).isDisplayed());
    }

    @Test
    public void importRuleChainAndDeleteFile() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.clearImportFileBtn().click();

        Assert.assertTrue(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
    }

    @Test
    public void importTxtFile() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        Assert.assertTrue(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
    }

    @Test
    public void importRuleChainAndSave() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);
        OpenRuleChainPageHelper openRuleChainPage = new OpenRuleChainPageHelper(driver);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        openRuleChainPage.doneBtn().click();
        openRuleChainPage.waitUntilDoneBtnDisable();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChain(IMPORT_RULE_CHAIN_NAME).isDisplayed());

        ruleChainsPage.deleteRuleChain(IMPORT_RULE_CHAIN_NAME);
    }

    @Test
    public void importRuleChainAndSaveWithSameName() {
        LoginPageHelper loginPage = new LoginPageHelper(driver);
        SideBarMenuViewElements sideBarMenuView = new SideBarMenuViewElements(driver);
        RuleChainsPageHelper ruleChainsPage = new RuleChainsPageHelper(driver);
        OpenRuleChainPageHelper openRuleChainPage = new OpenRuleChainPageHelper(driver);

        ruleChainsPage.createRuleChain(IMPORT_RULE_CHAIN_NAME);

        openUrl(URL);
        loginPage.authorizationTenant();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        openRuleChainPage.doneBtn().click();
        openRuleChainPage.waitUntilDoneBtnDisable();
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertTrue(ruleChainsPage.entities(IMPORT_RULE_CHAIN_NAME).size() > 1);

        ruleChainsPage.deleteAllRuleChain(IMPORT_RULE_CHAIN_NAME);
    }
}
