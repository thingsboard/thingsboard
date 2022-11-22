package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.*;

public class DeleteSeveralRuleChainsAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelperAbstract ruleChainsPage;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelperAbstract(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can mark several rule chains in the checkbox near the names and then click on the trash can icon in the menu that appears at the top")
    public void canDeleteSeveralRuleChainsByTopBtn() {
        String ruleChainName = ENTITY_NAME;
        int count = 2;
        ruleChainsPage.createRuleChains(ruleChainName, count);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.clickOnCheckBoxes(count);

        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainsIsNotPresent(ruleChainName));
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can mark several rule chains in the checkbox near the names and then click on the trash can icon in the menu that appears at the top")
    public void selectAllRuleChain() {
        String ruleChainName = ENTITY_NAME;
        int count = 2;
        ruleChainsPage.createRuleChains(ruleChainName, count);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();
        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainsIsNotPresent(ruleChainName));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t remove Root Rule Chain (the trash can is disabled in the right corner)")
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        Assert.assertFalse(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME).isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t remove Root Rule Chain (can`t mark the rule chain in the checkbox )")
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.selectAllCheckBox().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Test(priority = 30, groups = "smoke")
    @Description("The rule chains are deleted immediately after clicking remove (no need to refresh the page)")
    public void deleteSeveralRuleChainsByTopBtnWithoutRefresh() {
        String ruleChainName = ENTITY_NAME;
        int count = 2;
        ruleChainsPage.createRuleChains(ruleChainName, count);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.clickOnCheckBoxes(count);
        ruleChainsPage.deleteSelectedBtn().click();
        ruleChainsPage.warningPopUpYesBtn().click();

        Assert.assertTrue(ruleChainsPage.ruleChainsIsNotPresent(ruleChainName));
    }
}
