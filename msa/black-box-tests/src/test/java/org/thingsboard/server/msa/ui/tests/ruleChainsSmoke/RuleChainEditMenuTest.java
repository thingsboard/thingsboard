package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.*;

public class RuleChainEditMenuAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelperAbstract ruleChainsPage;
    private String ruleChainName;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelperAbstract(driver);
    }

    @AfterMethod
    public void delete() {
        if (ruleChainName != null) {
            ruleChainsPage.deleteRuleChain(ruleChainName);
            ruleChainName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can click by pencil icon and edit the name (change the name) and save the changes. All changes have been applied")
    public void changeName() {
        ruleChainsPage.createRuleChain(ENTITY_NAME);
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
    @Description("Can`t delete the name and save changes")
    public void deleteName() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.changeNameEditMenu("");

        Assert.assertFalse(ruleChainsPage.doneBtnEditViewVisible().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t save just a space in the name")
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
    @Description("Can write/change/delete the descriptionEntityView and save the changes. All changes have been applied")
    public void editDescription() {
        String name = ENTITY_NAME;
        ruleChainsPage.createRuleChain(name);
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
        ruleChainName = name;

        Assert.assertTrue(ruleChainsPage.descriptionEntityView().getAttribute("value").isEmpty());
        Assert.assertEquals(description, description1);
        Assert.assertEquals(description + description, description2);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can enable / disable debug and save changes. All changes have been applied")
    public void debugMode() {
        String name = ENTITY_NAME;
        ruleChainsPage.createRuleChain(name);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.notRootRuleChainsNames().get(0).click();
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();
        boolean debugMode = Boolean.parseBoolean(ruleChainsPage.debugCheckboxView().getAttribute("aria-checked"));
        ruleChainsPage.editPencilBtn().click();
        ruleChainsPage.debugCheckboxEdit().click();
        ruleChainsPage.doneBtnEditView().click();
        ruleChainName = name;

        Assert.assertFalse(Boolean.parseBoolean(ruleChainsPage.debugCheckboxView().getAttribute("aria-checked")));
        Assert.assertTrue(debugMode);
    }
}