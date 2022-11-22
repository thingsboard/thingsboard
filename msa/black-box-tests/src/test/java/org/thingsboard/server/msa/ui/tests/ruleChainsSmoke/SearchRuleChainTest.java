package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class SearchRuleChainAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private RuleChainsPageHelperAbstract ruleChainsPage;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelperAbstract(driver);
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "ruleChainNameForSearchByFirstAndSecondWord")
    @Description("Can search by the first/second word of the name")
    public void searchFirstWord(String namePath) {
        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchEntity(namePath);
        ruleChainsPage.setRuleChainName(0);

        Assert.assertTrue(ruleChainsPage.getRuleChainName().contains(namePath));
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSearchBySymbolAndNumber")
    @Description("Can search by number/symbol")
    public void searchNumber(String name, String namePath) {
        ruleChainsPage.createRuleChain(name);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.searchEntity(namePath);
        ruleChainsPage.setRuleChainName(0);
        boolean ruleChainContainsNamePath = ruleChainsPage.getRuleChainName().contains(namePath);

        ruleChainsPage.deleteRuleChain(name);

        Assert.assertTrue(ruleChainContainsNamePath);
    }
}
