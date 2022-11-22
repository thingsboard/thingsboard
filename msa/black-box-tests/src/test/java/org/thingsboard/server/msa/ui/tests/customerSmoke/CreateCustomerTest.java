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

import static org.thingsboard.server.msa.ui.utils.Const.*;

public class CreateCustomerAbstractDiverBaseTest extends AbstractDiverBaseTest {

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

    @Test(priority = 10, groups = "smoke")
    @Description("Can click on Add after specifying the name (text/numbers /special characters)")
    public void createCustomer() {
        String customerName = ENTITY_NAME;

        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
        customerPage.addBtnC().click();
        customerPage.refreshBtn().click();
        this.customerName = customerName;

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertTrue(customerPage.customer(customerName).isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    public void createCustomerWithFullInformation() {
        String customerName = ENTITY_NAME;
        String text = "Text";
        String email = "email@mail.com";
        String number = "2015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
        customerPage.selectCountryAddEntityView();
        customerPage.descriptionAddEntityView().sendKeys(text);
        customerPage.cityAddEntityView().sendKeys(text);
        customerPage.stateAddEntityView().sendKeys(text);
        customerPage.zipAddEntityView().sendKeys(text);
        customerPage.addressAddEntityView().sendKeys(text);
        customerPage.address2AddEntityView().sendKeys(text);
        customerPage.phoneNumberAddEntityView().sendKeys(number);
        customerPage.emailAddEntityView().sendKeys(email);
        customerPage.addBtnC().click();
        customerPage.setCustomerEmail(customerName);
        customerPage.setCustomerCountry(customerName);
        customerPage.setCustomerCity(customerName);
        customerPage.entity(customerName).click();
        this.customerName = customerName;

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertEquals(customerPage.entityViewTitle().getText(), customerName);
        Assert.assertEquals(customerPage.titleFieldEntityView().getAttribute("value"), customerName);
        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
        Assert.assertEquals(customerPage.descriptionEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.cityEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.stateEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.zipEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.addressEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.address2EntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "+1" + number);
        Assert.assertEquals(customerPage.emailEntityView().getAttribute("value"), email);
        Assert.assertEquals(customerPage.getCustomerEmail(), email);
        Assert.assertEquals(customerPage.getCustomerCountry(), customerPage.getCountry());
        Assert.assertEquals(customerPage.getCustomerCity(), text);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t add customer without the name (empty field or just space)")
    public void createCustomerWithoutName() {
        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();

        Assert.assertFalse(customerPage.addBtnV().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description()
    public void createCustomerWithOnlySpace() {
        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(" ");
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityView());
        Assert.assertTrue(customerPage.addEntityView().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can't create a customer with the same name")
    public void createCustomerSameName() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        String customerName = customerPage.getCustomerName();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
        customerPage.addBtnC().click();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), SAME_NAME_WARNING_CUSTOMER_MESSAGE);
        Assert.assertNotNull(customerPage.addEntityView());
        Assert.assertTrue(customerPage.addEntityView().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can click on Add after specifying the name (text/numbers /special characters)")
    public void createCustomerWithoutRefresh() {
        String customerName = ENTITY_NAME;

        sideBarMenuView.customerBtn().click();
        customerPage.plusBtn().click();
        customerPage.titleFieldAddEntityView().sendKeys(customerName);
        customerPage.addBtnC().click();
        this.customerName = customerName;

        Assert.assertNotNull(customerPage.customer(customerName));
        Assert.assertTrue(customerPage.customer(customerName).isDisplayed());
    }

    @Test(priority = 40, groups = "smoke")
    @Description("Question mark icon leads to rule chain documentation (PE)")
    public void documentation() {
        String urlPath = "docs/user-guide/ui/customers/";

        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.customer(customerPage.getCustomerName()).click();
        customerPage.goToHelpPage();

        Assert.assertTrue(urlContains(urlPath));
    }
}
