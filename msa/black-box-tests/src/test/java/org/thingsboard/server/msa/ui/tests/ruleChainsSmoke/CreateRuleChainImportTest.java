package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.OpenRuleChainPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.*;

public class CreateRuleChainImportAbstractDiverBaseTest extends AbstractDiverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelperAbstract ruleChainsPage;
    private OpenRuleChainPageHelperAbstract openRuleChainPage;
    private final String absolutePathToFileImportRuleChain = getClass().getClassLoader().getResource(IMPORT_RULE_CHAIN_FILE_NAME).getPath();
    private final String absolutePathToFileImportTxt = getClass().getClassLoader().getResource(IMPORT_TXT_FILE_NAME).getPath();
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelperAbstract(driver);
        openRuleChainPage = new OpenRuleChainPageHelperAbstract(driver);
    }

    @AfterMethod
    public void delete() {
        if (ruleChainName != null) {
            ruleChainsPage.deleteRuleChain(ruleChainName);
            ruleChainName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can drop a JSON file and import it")
    public void importRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);

        Assert.assertNotNull(ruleChainsPage.importingFile(IMPORT_RULE_CHAIN_FILE_NAME));
        Assert.assertTrue(ruleChainsPage.importingFile(IMPORT_RULE_CHAIN_FILE_NAME).isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can delete a file by clicking on the icon Remove")
    public void importRuleChainAndDeleteFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.clearImportFileBtn().click();

        Assert.assertNotNull(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(IMPORT_TXT_FILE_NAME));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t Select / drop a file of a different format than JSON")
    public void importTxtFile() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportTxt);

        Assert.assertNotNull(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE));
        Assert.assertTrue(ruleChainsPage.importingFile(EMPTY_IMPORT_MESSAGE).isDisplayed());
    }

    @Test(priority = 30, groups = "smoke")
    @Description("After clicking on Import - imported rule chain opens (need to save by clicking on the Apply changes icon)")
    public void importRuleChainAndSave() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        openRuleChainPage.doneBtn().click();
        openRuleChainPage.waitUntilDoneBtnDisable();
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainName = IMPORT_RULE_CHAIN_NAME;

        Assert.assertNotNull(ruleChainsPage.entity(IMPORT_RULE_CHAIN_NAME));
        Assert.assertTrue(ruleChainsPage.entity(IMPORT_RULE_CHAIN_NAME).isDisplayed());
    }

    @Test(priority = 40, groups = "smoke")
    @Description("Can create a rule chain with the same name")
    public void importRuleChainAndSaveWithSameName() {
        ruleChainsPage.createRuleChain(IMPORT_RULE_CHAIN_NAME);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openImportRuleChainView();
        ruleChainsPage.browseFile().sendKeys(absolutePathToFileImportRuleChain);
        ruleChainsPage.importBrowseFileBtn().click();
        openRuleChainPage.doneBtn().click();
        openRuleChainPage.waitUntilDoneBtnDisable();
        sideBarMenuView.ruleChainsBtn().click();
        boolean sizeBigger1 = ruleChainsPage.entities(IMPORT_RULE_CHAIN_NAME).size() > 1;

        ruleChainsPage.deleteAllRuleChain(IMPORT_RULE_CHAIN_NAME);

        Assert.assertTrue(sizeBigger1);
    }
}
