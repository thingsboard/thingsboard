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

public class SearchCustomerAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelperAbstract customerPage;
    private String entityName;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelperAbstract(driver);
    }

    @AfterMethod
    public void deleteCustomer() {
        customerPage.deleteCustomer(entityName);
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "customerNameForSearchByFirstAndSecondWord")
    @Description("Can search by the first/second word of the name")
    public void searchFirstWord(String namePath) {
        sideBarMenuView.customerBtn().click();
        customerPage.searchEntity(namePath);

        customerPage.allEntity().forEach(x -> Assert.assertTrue(x.getText().contains(namePath)));
    }

    @Test(priority = 10, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "nameForSearchBySymbolAndNumber")
    @Description("Can search by number/symbol")
    public void searchNumber(String name, String namePath) {
        customerPage.createCustomer(name);

        sideBarMenuView.customerBtn().click();
        customerPage.searchEntity(namePath);
        customerPage.setCustomerName();
        entityName = name;

        Assert.assertTrue(customerPage.getCustomerName().contains(namePath));
    }
}
