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

public class DeleteRuleChainAbstractDiverBaseTest extends AbstractDiverBaseTest {
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
    @Description("Can remove the rule chain by clicking on the trash can icon in the right corner")
    public void removeRuleChainByRightSideBtn() {
        ruleChainsPage.createRuleChain(ENTITY_NAME);

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ENTITY_NAME);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedRuleChain));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can mark the rule chain in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedRuleChain() {
        String ruleChainName = ENTITY_NAME;
        ruleChainsPage.createRuleChain(ruleChainName);

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected(ruleChainName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedRuleChain));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can click on the name of the rule chain and click on the 'Delete rule chain' button")
    public void removeFromRuleChainView() {
        ruleChainsPage.createRuleChain(ENTITY_NAME);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entity(ENTITY_NAME).click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainFromView(ENTITY_NAME);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedRuleChain));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t remove Root Rule Chain (the trash can is disabled in the right corner)")
    public void removeRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        Assert.assertFalse(ruleChainsPage.deleteBtn(ROOT_RULE_CHAIN_NAME).isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t remove Root Rule Chain (can`t mark the rule chain in the checkbox )")
    public void removeSelectedRootRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();

        ruleChainsPage.assertCheckBoxIsNotDisplayed(ROOT_RULE_CHAIN_NAME);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t remove Root Rule Chain (missing delete button)")
    public void removeFromRootRuleChainView() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entity(ROOT_RULE_CHAIN_NAME).click();
        ruleChainsPage.deleteBtnFromView();

        ruleChainsPage.assertDeleteBtnInRootRuleChainIsNotDisplayed();
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can remove the rule chain by clicking on the trash can icon in the right corner")
    public void removeProfileRuleChainByRightSideBtn() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.deleteBtn(deletedRuleChain).click();
        ruleChainsPage.warningPopUpYesBtn().click();
        ruleChainsPage.refreshBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertTrue(ruleChainsPage.entity(deletedRuleChain).isDisplayed());
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can mark the rule chain in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedProfileRuleChain() {
        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteSelected("Thermostat");
        ruleChainsPage.refreshBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertTrue(ruleChainsPage.entity(deletedRuleChain).isDisplayed());
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can click on the name of the rule chain and click on the 'Delete rule chain' button")
    public void removeFromProfileRuleChainView() {
        String deletedRuleChain = "Thermostat";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.entity(deletedRuleChain).click();
        ruleChainsPage.deleteBtnFromView().click();
        ruleChainsPage.warningPopUpYesBtn().click();

        Assert.assertNotNull(ruleChainsPage.entity(deletedRuleChain));
        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), DELETE_RULE_CHAIN_WITH_PROFILE_MESSAGE);
    }

    @Test(priority = 30, groups = "smoke")
    @Description("The rule chain is deleted immediately after clicking remove (no need to refresh the page)")
    public void removeRuleChainByRightSideBtnWithoutRefresh() {
        String ruleChainName = ENTITY_NAME;
        ruleChainsPage.createRuleChain(ruleChainName);

        sideBarMenuView.ruleChainsBtn().click();
        String deletedRuleChain = ruleChainsPage.deleteRuleChainTrash(ruleChainName);

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedRuleChain));
    }
}
