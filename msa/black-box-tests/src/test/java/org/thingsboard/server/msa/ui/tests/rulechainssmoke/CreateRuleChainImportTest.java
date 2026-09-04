// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.tests.rulechainssmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import org.openqa.selenium.WebElement;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_IMPORT_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_RULE_CHAIN_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.IMPORT_TXT_FILE_NAME;
import static org.thingsboard.server.msa.ui.utils.EntityPrototypes.defaultRuleChainPrototype;

@Feature("Import rule chain")
public class CreateRuleChainImportTest extends AbstractRuleChainTest {

    private final String absolutePathToFileImportRuleChain = getClass().getClassLoader().getResource(IMPORT_RULE_CHAIN_FILE_NAME).getPath();
    private final String absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();

    @Test(priority = 10, groups = "smoke")
    @Description("Drop json file")
    public void importRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);

        assertIsDisplayed(ruleChainsPage.importingFile(IMPORT_RULE_CHAIN_FILE_NAME));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Drop json file and delete it")
    public void importRuleChainAndDeleteFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.clearImportFileBtn().click();

        assertIsDisplayed(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
        ruleChainsPage.assertEntityIsNotPresent(IMPORT_RULE_CHAIN_FILE_NAME);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Import txt file")
    public void importTxtFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        assertIsDisplayed(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
    }

    @Test(priority = 30, groups = "smoke")
    @Description("Import rule chain")
    public void importRuleChainAndSave() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        WebElement doneBtn = openRuleChainPage.doneBtn();
        doneBtn.click();
        ruleChainName = IMPORT_RULE_CHAIN_NAME;
        sideBarMenuView.ruleChainsBtn().click();

        assertIsDisplayed(ruleChainsPage.entity(ruleChainName));
    }

    @Test(priority = 40, groups = "smoke")
    @Description("Import rule chain with same name")
    public void importRuleChainAndSaveWithSameName() {
        ruleChainName = IMPORT_RULE_CHAIN_NAME;
        testRestClient.postRuleChain(defaultRuleChainPrototype(ruleChainName));

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        WebElement doneBtn = openRuleChainPage.doneBtn();
        doneBtn.click();
        sideBarMenuView.ruleChainsBtn().click();

        assertThat(ruleChainsPage.entities(ruleChainName).size() > 1).
                as("More than 1 rule chains have been created").isTrue();
        ruleChainsPage.entities(ruleChainName).forEach(this::assertIsDisplayed);
    }
}
