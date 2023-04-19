package org.thingsboard.server.msa.ui.tests.ruleChainsSmoke;

import io.qameta.allure.Epic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.OpenRuleChainPageHelper;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

@Epic("Rule chains smoke tests")
abstract public class AbstractRuleChainTest extends AbstractDriverBaseTest {

    protected SideBarMenuViewElements sideBarMenuView;
    protected RuleChainsPageHelper ruleChainsPage;
    protected OpenRuleChainPageHelper openRuleChainPage;
    protected String ruleChainName;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        ruleChainsPage = new RuleChainsPageHelper(driver);
        openRuleChainPage = new OpenRuleChainPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        deleteRuleChainByName(ruleChainName);
        ruleChainName = null;
    }
}

