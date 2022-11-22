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

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class SortByTimeAbstractDiverBaseTest extends AbstractDiverBaseTest {

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
    @Description
    public void sortByTimeDown() {
        String ruleChain = ENTITY_NAME;
        ruleChainsPage.createRuleChain(ruleChain);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(0).getText();
        ruleChainName = ruleChain;

        Assert.assertEquals(firstListElement, lastCreated);
        Assert.assertNotNull(ruleChainsPage.createdTimeEntity(ruleChain, lastCreated));
    }

    @Test(priority = 10, groups = "smoke")
    @Description
    public void sortByTimeUp() {
        String ruleChain = ENTITY_NAME;
        ruleChainsPage.createRuleChain(ruleChain);

        sideBarMenuView.ruleChainsBtn().click();
        ruleChainsPage.sortByTimeBtn().click();
        ruleChainsPage.setSort();
        String firstListElement = ruleChainsPage.getSort().get(ruleChainsPage.getSort().size() - 1);
        String lastCreated = ruleChainsPage.createdTime().get(ruleChainsPage.createdTime().size() - 1).getText();
        ruleChainName = ruleChain;

        Assert.assertEquals(firstListElement, lastCreated);
        Assert.assertNotNull(ruleChainsPage.createdTimeEntity(ruleChain, lastCreated));
    }
}
