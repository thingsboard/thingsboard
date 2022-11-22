package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.utils.Const.URL;

public class SortByNameAbstractDiverBaseTest extends AbstractDiverBaseTest {
    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelperAbstract customerPage;
    private String customerName;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelperAbstract(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            customerPage.deleteCustomer(customerName);
            customerName = null;
        }
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterUp(String title) {
        customerPage.createCustomer(title);

        sideBarMenuView.customerBtn().click();
        customerPage.sortByTitleBtn().click();
        customerPage.setCustomerName();
        customerName = title;

        Assert.assertEquals(customerPage.getCustomerName(), title);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortUp(String customer, String customerSymbol, String customerNumber) {
        customerPage.createCustomer(customerSymbol);
        customerPage.createCustomer(customer);
        customerPage.createCustomer(customerNumber);

        sideBarMenuView.customerBtn().click();
        customerPage.sortByTitleBtn().click();
        customerPage.setCustomerName(0);
        String firstCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(1);
        String secondCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(2);
        String thirdCustomer = customerPage.getCustomerName();

        boolean firstEquals = firstCustomer.equals(customerSymbol);
        boolean secondEquals = secondCustomer.equals(customerNumber);
        boolean thirdEquals = thirdCustomer.equals(customer);

        customerPage.deleteCustomer(customer);
        customerPage.deleteCustomer(customerNumber);
        customerPage.deleteCustomer(customerSymbol);

        Assert.assertTrue(firstEquals);
        Assert.assertTrue(secondEquals);
        Assert.assertTrue(thirdEquals);
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSort")
    @Description
    public void specialCharacterDown(String title) {
        customerPage.createCustomer(title);

        sideBarMenuView.customerBtn().click();
        customerPage.sortByNameDown();
        customerPage.setCustomerName(customerPage.allEntity().size() - 1);
        customerName = title;

        Assert.assertEquals(customerPage.getCustomerName(), title);
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForAllSort")
    @Description
    public void allSortDown(String customer, String customerSymbol, String customerNumber) {
        customerPage.createCustomer(customerSymbol);
        customerPage.createCustomer(customer);
        customerPage.createCustomer(customerNumber);

        sideBarMenuView.customerBtn().click();
        int lastIndex = customerPage.allEntity().size() - 1;
        customerPage.sortByNameDown();
        customerPage.setCustomerName(lastIndex);
        String firstCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(lastIndex - 1);
        String secondCustomer = customerPage.getCustomerName();
        customerPage.setCustomerName(lastIndex - 2);
        String thirdCustomer = customerPage.getCustomerName();

        boolean firstEquals = firstCustomer.equals(customerSymbol);
        boolean secondEquals = secondCustomer.equals(customerNumber);
        boolean thirdEquals = thirdCustomer.equals(customer);

        customerPage.deleteCustomer(customer);
        customerPage.deleteCustomer(customerNumber);
        customerPage.deleteCustomer(customerSymbol);

        Assert.assertTrue(firstEquals);
        Assert.assertTrue(secondEquals);
        Assert.assertTrue(thirdEquals);
    }
}
