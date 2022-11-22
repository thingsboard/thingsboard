package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.RuleChainsPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class DeleteCustomerAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelperAbstract customerPage;
    private RuleChainsPageHelperAbstract ruleChainsPage;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelperAbstract(driver);
        ruleChainsPage = new RuleChainsPageHelperAbstract(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can remove the customer by clicking on the trash can icon in the right corner")
    public void removeCustomerByRightSideBtn() {
        String customer = ENTITY_NAME;
        customerPage.createCustomer(customer);

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customer);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.entityIsNotPresent(deletedCustomer));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can mark the customer in the checkbox and then click on the trash can icon in the menu that appears at the top")
    public void removeSelectedCustomer() {
        String customerName = ENTITY_NAME;
        customerPage.createCustomer(customerName);

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteSelected(customerName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedCustomer));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can click on the name of the rule chain and click on the 'Delete customer' button")
    public void removeFromCustomerView() {
        String customerName = ENTITY_NAME;
        customerPage.createCustomer(customerName);

        sideBarMenuView.customerBtn().click();
        customerPage.entity(customerName).click();
        String deletedCustomer = ruleChainsPage.deleteRuleChainFromView(customerName);
        ruleChainsPage.refreshBtn().click();

        Assert.assertTrue(ruleChainsPage.entityIsNotPresent(deletedCustomer));
    }

    @Test(priority = 20, groups = "smoke")
    @Description("The rule chain is deleted immediately after clicking remove (no need to refresh the page)")
    public void removeCustomerByRightSideBtnWithoutRefresh() {
        String customer = ENTITY_NAME;
        customerPage.createCustomer(customer);

        sideBarMenuView.customerBtn().click();
        String deletedCustomer = customerPage.deleteRuleChainTrash(customer);
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.entityIsNotPresent(deletedCustomer));
    }
}
