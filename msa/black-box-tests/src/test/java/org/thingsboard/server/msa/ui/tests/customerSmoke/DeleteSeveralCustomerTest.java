package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;

import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;
import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class DeleteSeveralCustomerAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelperAbstract customerPage;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelperAbstract(driver);
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can mark several customers in the checkbox near the names and then click on the trash can icon in the menu that appears at the top")
    public void canDeleteSeveralCustomersByTopBtn() {
        String title1 = ENTITY_NAME + "1";
        String title2 = ENTITY_NAME + "2";
        int count = 2;
        customerPage.createCustomer(title1);
        customerPage.createCustomer(title2);

        sideBarMenuView.customerBtn().click();
        customerPage.clickOnCheckBoxes(count);

        Assert.assertEquals(customerPage.markCheckbox().size(), count);
        customerPage.markCheckbox().forEach(x -> Assert.assertTrue(x.isDisplayed()));

        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();
        customerPage.refreshBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(title1));
        Assert.assertTrue(customerPage.customerIsNotPresent(title2));
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can mark several rule chains in the checkbox near the names and then click on the trash can icon in the menu that appears at the top")
    public void selectAllCustomers() {
        sideBarMenuView.customerBtn().click();
        customerPage.selectAllCheckBox().click();
        customerPage.deleteSelectedBtn().click();

        Assert.assertNotNull(customerPage.warningPopUpTitle());
        Assert.assertTrue(customerPage.warningPopUpTitle().isDisplayed());
        Assert.assertTrue(customerPage.warningPopUpTitle().getText().contains(String.valueOf(customerPage.markCheckbox().size())));
    }

    @Test(priority = 30, groups = "smoke")
    @Description("The rule chains are deleted immediately after clicking remove (no need to refresh the page)")
    public void deleteSeveralCustomersByTopBtnWithoutRefresh() {
        String title1 = ENTITY_NAME + "1";
        String title2 = ENTITY_NAME + "2";
        int count = 2;
        customerPage.createCustomer(title1);
        customerPage.createCustomer(title2);

        sideBarMenuView.customerBtn().click();
        customerPage.clickOnCheckBoxes(count);

        customerPage.deleteSelectedBtn().click();
        customerPage.warningPopUpYesBtn().click();

        Assert.assertTrue(customerPage.customerIsNotPresent(title1));
        Assert.assertTrue(customerPage.customerIsNotPresent(title2));
    }
}
