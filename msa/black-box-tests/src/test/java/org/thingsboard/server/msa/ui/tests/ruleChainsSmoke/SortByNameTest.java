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
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class SortByNameAbstractDiverBaseTest extends AbstractDiverBaseTest {

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

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterUp(String ruleChainName) {
        ruleChainsPage.createRuleChain(ruleChainName);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameBtn().click();
        ruleChainsPage.setRuleChainName(0);
        this.ruleChainName = ruleChainName;

        Assert.assertEquals(ruleChainsPage.getRuleChainName(), ruleChainName);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortUp(String ruleChain, String ruleChainSymbol, String ruleChainNumber) {
        ruleChainsPage.createRuleChain(ruleChainSymbol);
        ruleChainsPage.createRuleChain(ruleChain);
        ruleChainsPage.createRuleChain(ruleChainNumber);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameBtn().click();
        ruleChainsPage.setRuleChainName(0);
        String firstRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(1);
        String secondRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(2);
        String thirdRuleChain = ruleChainsPage.getRuleChainName();

        boolean firstEquals = firstRuleChain.equals(ruleChainSymbol);
        boolean secondEquals = secondRuleChain.equals(ruleChainNumber);
        boolean thirdEquals = thirdRuleChain.equals(ruleChain);

        ruleChainsPage.deleteRuleChain(ruleChain);
        ruleChainsPage.deleteRuleChain(ruleChainNumber);
        ruleChainsPage.deleteRuleChain(ruleChainSymbol);

        Assert.assertTrue(firstEquals);
        Assert.assertTrue(secondEquals);
        Assert.assertTrue(thirdEquals);
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterDown(String ruleChainName) {
        ruleChainsPage.createRuleChain(ruleChainName);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByNameDown();
        ruleChainsPage.setRuleChainName(ruleChainsPage.allNames().size() - 1);
        this.ruleChainName = ruleChainName;

        Assert.assertEquals(ruleChainsPage.getRuleChainName(), ruleChainName);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortDown(String ruleChain, String ruleChainSymbol, String ruleChainNumber) {
        ruleChainsPage.createRuleChain(ruleChainSymbol);
        ruleChainsPage.createRuleChain(ruleChain);
        ruleChainsPage.createRuleChain(ruleChainNumber);

        sideBarMenuView.ruleChainsBtn().click();
        int lastIndex = ruleChainsPage.allNames().size() - 1;
        ruleChainsPage.sortByNameDown();
        ruleChainsPage.setRuleChainName(lastIndex);
        String firstRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(lastIndex - 1);
        String secondRuleChain = ruleChainsPage.getRuleChainName();
        ruleChainsPage.setRuleChainName(lastIndex - 2);
        String thirdRuleChain = ruleChainsPage.getRuleChainName();

        boolean firstEquals = firstRuleChain.equals(ruleChainSymbol);
        boolean secondEquals = secondRuleChain.equals(ruleChainNumber);
        boolean thirdEquals = thirdRuleChain.equals(ruleChain);

        ruleChainsPage.deleteRuleChain(ruleChain);
        ruleChainsPage.deleteRuleChain(ruleChainNumber);
        ruleChainsPage.deleteRuleChain(ruleChainSymbol);

        Assert.assertTrue(firstEquals);
        Assert.assertTrue(secondEquals);
        Assert.assertTrue(thirdEquals);
    }
}