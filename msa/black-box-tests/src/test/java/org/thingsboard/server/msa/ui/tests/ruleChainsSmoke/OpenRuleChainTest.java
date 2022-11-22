package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.OpenRuleChainPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class OpenRuleChainAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelperAbstract ruleChainsPage;
    private OpenRuleChainPageHelperAbstract openRuleChainPage;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelperAbstract(driver);
        openRuleChainPage = new OpenRuleChainPageHelperAbstract(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can open the rule chain by clicking on the 'Open rule chain' icon in the right corner")
    public void openRuleChainByRightCornerBtn() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.openRuleChainBtn(ruleChain).click();
        openRuleChainPage.setHeadName();

        Assert.assertTrue(urlContains(ruleChainsPage.getRuleChainId(ruleChainsPage.getRuleChainName())));
        Assert.assertTrue(openRuleChainPage.headRuleChainName().isDisplayed());
        Assert.assertTrue(openRuleChainPage.inputNode().isDisplayed());
        Assert.assertEquals(ruleChainsPage.getRuleChainName(), openRuleChainPage.getHeadName());
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can open the rule chain by clicking on the name/row of the rule chain and click on the 'Open rule chain' button")
    public void openRuleChainByViewBtn() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.entity(ruleChain).click();
        ruleChainsPage.openRuleChainFromViewBtn().click();
        openRuleChainPage.setHeadName();

        Assert.assertTrue(urlContains(ruleChainsPage.getRuleChainId(ruleChain)));
        Assert.assertTrue(openRuleChainPage.headRuleChainName().isDisplayed());
        Assert.assertTrue(openRuleChainPage.inputNode().isDisplayed());
        Assert.assertEquals(ruleChain, openRuleChainPage.getHeadName());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t open the rule chain by clicking twice on the row/name")
    public void openRuleChainDoubleClick() {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setRuleChainNameWithoutRoot(0);
        String ruleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.doubleClickOnRuleChain(ruleChain);

        Assert.assertEquals(getUrl(), URL + "ruleChains");
    }
}
