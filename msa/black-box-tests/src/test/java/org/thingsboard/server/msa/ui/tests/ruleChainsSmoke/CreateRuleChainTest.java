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

public class CreateRuleChainAbstractDiverBaseTest extends AbstractDiverBaseTest {

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
    @Description("Can click on Add after specifying the name (text/numbers /special characters)")
    public void createRuleChain() {
        String ruleChainName = ENTITY_NAME;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();
        this.ruleChainName = ruleChainName;

        Assert.assertNotNull(ruleChainsPage.entity(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());
    }

    @Test(priority = 10, groups = "smoke")
    @Description()
    public void createRuleChainWithDescription() {
        String ruleChainName = ENTITY_NAME;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.descriptionAddEntityView().sendKeys(ENTITY_NAME);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();
        ruleChainsPage.entity(ENTITY_NAME).click();
        ruleChainsPage.setHeaderName();
        this.ruleChainName = ruleChainName;

        Assert.assertEquals(ruleChainsPage.getHeaderName(), ruleChainName);
        Assert.assertEquals(ruleChainsPage.descriptionEntityView().getAttribute("value"), ruleChainName);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t add rule chain without the name (empty field or just space)")
    public void createRuleChainWithoutName() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();

        Assert.assertFalse(ruleChainsPage.addBtnV().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description()
    public void createRuleChainWithOnlySpace() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(" ");
        ruleChainsPage.addBtnC().click();

        Assert.assertNotNull(ruleChainsPage.warningMessage());
        Assert.assertTrue(ruleChainsPage.warningMessage().isDisplayed());
        Assert.assertEquals(ruleChainsPage.warningMessage().getText(), EMPTY_RULE_CHAIN_MESSAGE);
        Assert.assertNotNull(ruleChainsPage.addEntityView());
        Assert.assertTrue(ruleChainsPage.addEntityView().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can create a rule chain with the same name")
    public void createRuleChainWithSameName() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot();
        ruleChainsPage.openCreateRuleChainView();
        String ruleChainName = ruleChainsPage.getRuleChainName();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        ruleChainsPage.refreshBtn().click();
        this.ruleChainName = ruleChainName;

        Assert.assertNotNull(ruleChainsPage.entity(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entities(ruleChainName).size() > 1);
    }

    @Test(priority = 30, groups = "smoke")
    @Description("After clicking on Add - appears immediately in the list (no need to refresh the page)")
    public void createRuleChainWithoutRefresh() {
        String ruleChainName = ENTITY_NAME;

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.openCreateRuleChainView();
        ruleChainsPage.nameField().sendKeys(ruleChainName);
        ruleChainsPage.addBtnC().click();
        this.ruleChainName = ruleChainName;

        Assert.assertNotNull(ruleChainsPage.entity(ruleChainName));
        Assert.assertTrue(ruleChainsPage.entity(ruleChainName).isDisplayed());
    }

    @Test(priority = 40, groups = "smoke")
    @Description("Question mark icon leads to rule chain documentation (PE)")
    public void documentation() {
        String urlPath = "docs/user-guide/ui/rule-chains/";

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot();
        ruleChainsPage.entity(ruleChainsPage.getRuleChainName()).click();
        ruleChainsPage.goToHelpPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}
