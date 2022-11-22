package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class ManageCustomersAssetsAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelperAbstract customerPage;
    private final String manage = "Assets";

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelperAbstract(driver);
    }

    @Test(groups = "smoke")
    @Description("Can go to the 'Customer assets' window by clicking on the 'Manage customer users' icon in the right corner")
    public void openWindowByRightCornerBtn() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.manageCustomersAssetsBtn(customerPage.getCustomerName()).click();

        Assert.assertTrue(urlContains(manage.toLowerCase()));
        Assert.assertNotNull(customerPage.customerAssetsIconHeader());
        Assert.assertTrue(customerPage.customerAssetsIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(manage));
    }

    @Test(groups = "smoke")
    @Description("Can go to the 'Customer Assets' window by clicking on the name/row of the customer and click on the 'Manage users' button")
    public void openWindowByView() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.entity(customerPage.getCustomerName()).click();
        customerPage.manageCustomersAssetsBtnView().click();

        Assert.assertTrue(urlContains(manage.toLowerCase()));
        Assert.assertNotNull(customerPage.customerAssetsIconHeader());
        Assert.assertTrue(customerPage.customerAssetsIconHeader().isDisplayed());
        Assert.assertTrue(customerPage.customerManageWindowIconHead().getText().contains(manage));
    }
}
