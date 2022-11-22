package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.thingsboard.server.msa.ui.base.AbstractDiverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.DashboardPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.LoginPageHelperAbstract;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewElements;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.utils.Const.*;

public class CustomerEditMenuAbstractDiverBaseTest extends AbstractDiverBaseTest {

    private SideBarMenuViewElements sideBarMenuView;
    private CustomerPageHelperAbstract customerPage;
    private DashboardPageHelperAbstract dashboardPage;
    private String customerName;

    @BeforeMethod
    public void login() {
        openUrl(URL);
        new LoginPageHelperAbstract(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewElements(driver);
        customerPage = new CustomerPageHelperAbstract(driver);
        dashboardPage = new DashboardPageHelperAbstract(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerName != null) {
            customerPage.deleteCustomer(customerName);
            customerName = null;
        }
    }

    @Test(priority = 10, groups = "smoke")
    @Description("Can click by pencil icon and edit the title (change the title) and save the changes. All changes have been applied")
    public void changeTitle() {
        customerPage.createCustomer(ENTITY_NAME);
        String title = "Changed" + getRandomNumber();

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.setHeaderName();
        String titleBefore = customerPage.getHeaderName();
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(title);
        customerPage.doneBtnEditView().click();
        customerPage.setHeaderName();
        String titleAfter = customerPage.getHeaderName();
        customerName = title;

        Assert.assertNotEquals(titleBefore, titleAfter);
        Assert.assertEquals(titleAfter, title);
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t delete the title and save changes")
    public void deleteTitle() {
        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.titleFieldEntityView().clear();

        Assert.assertFalse(customerPage.doneBtnEditViewVisible().isEnabled());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can`t save just a space in the title")
    public void saveOnlyWithSpace() {
        sideBarMenuView.customerBtn().click();
        customerPage.setCustomerName();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.changeTitleEditMenu(" ");
        customerPage.doneBtnEditView().click();
        customerPage.setHeaderName();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_CUSTOMER_MESSAGE);
        Assert.assertEquals(customerPage.getCustomerName(), customerPage.getHeaderName());
    }

    @Test(priority = 20, groups = "smoke")
    @Description("Can write/change/delete the descriptionEntityView and save the changes. All changes have been applied")
    public void editDescription() {
        String title = ENTITY_NAME;
        customerPage.createCustomer(title);
        String description = "Description";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(description);
        customerPage.doneBtnEditView().click();
        String description1 = customerPage.descriptionEntityView().getAttribute("value");
        customerPage.editPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(description);
        customerPage.doneBtnEditView().click();
        String description2 = customerPage.descriptionEntityView().getAttribute("value");
        customerPage.editPencilBtn().click();
        customerPage.changeDescription("");
        customerPage.doneBtnEditView().click();
        customerName = title;

        Assert.assertEquals(description, description1);
        Assert.assertEquals(description + description, description2);
        Assert.assertTrue(customerPage.descriptionEntityView().getAttribute("value").isEmpty());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void assignedDashboardFromDashboard() {
        String title = ENTITY_NAME;
        customerPage.createCustomer(title);

        sideBarMenuView.dashboardBtn().click();
        dashboardPage.setDashboardTitle();
        dashboardPage.assignedBtn(dashboardPage.getDashboardTitle()).click();
        dashboardPage.assignedCustomer(title);
        sideBarMenuView.customerBtn().click();
        customerPage.entity(title).click();
        customerPage.editPencilBtn().click();
        customerPage.chooseDashboard();
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        customerPage.manageCustomersUserBtn(title).click();
        customerPage.createCustomersUser();
        customerPage.userLoginBtn().click();
        customerName = title;

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboardFromView(), dashboardPage.getDashboardTitle());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void assignedDashboard() {
        String title = ENTITY_NAME;
        customerPage.createCustomer(title);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDashboardsBtn(title).click();
        customerPage.assignedDashboard();
        sideBarMenuView.customerBtn().click();
        customerPage.entity(title).click();
        customerPage.editPencilBtn().click();
        customerPage.chooseDashboard();
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        customerPage.manageCustomersUserBtn(title).click();
        customerPage.createCustomersUser();
        customerPage.userLoginBtn().click();
        customerName = title;

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboard(), customerPage.getDashboardFromView());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void assignedDashboardWithoutHide() {
        String title = ENTITY_NAME;
        customerPage.createCustomer(title);

        sideBarMenuView.customerBtn().click();
        customerPage.manageCustomersDashboardsBtn(title).click();
        customerPage.assignedDashboard();
        sideBarMenuView.customerBtn().click();
        customerPage.entity(title).click();
        customerPage.editPencilBtn().click();
        customerPage.chooseDashboard();
        customerPage.hideHomeDashboardToolbarCheckbox().click();
        customerPage.doneBtnEditView().click();
        customerPage.setDashboardFromView();
        customerPage.closeEntityViewBtn().click();
        customerPage.manageCustomersUserBtn(title).click();
        customerPage.createCustomersUser();
        customerPage.userLoginBtn().click();
        customerName = title;

        Assert.assertNotNull(customerPage.usersWidget());
        Assert.assertTrue(customerPage.usersWidget().isDisplayed());
        Assert.assertEquals(customerPage.getDashboard(), customerPage.getDashboardFromView());
        Assert.assertNotNull(customerPage.stateController());
        Assert.assertNotNull(customerPage.filterBtn());
        Assert.assertNotNull(customerPage.timeBtn());
        Assert.assertTrue(customerPage.stateController().isDisplayed());
        Assert.assertTrue(customerPage.filterBtn().isDisplayed());
        Assert.assertTrue(customerPage.timeBtn().isDisplayed());
    }

    @Test(priority = 20, groups = "smoke")
    @Description
    public void addPhoneNumber() {
        String title = ENTITY_NAME;
        customerPage.createCustomer(title);
        String number = "2015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().sendKeys(number);
        customerPage.doneBtnEditView().click();
        customerName = title;

        Assert.assertTrue(customerPage.phoneNumberEntityView().getAttribute("value").contains(number));
    }

    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "incorrectPhoneNumber")
    @Description
    public void addIncorrectPhoneNumber(String number) {
        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.phoneNumberEntityView().sendKeys(number);
        boolean doneBtnIsEnable = customerPage.doneBtnEditViewVisible().isEnabled();
        customerPage.doneBtnEditViewVisible().click();

        Assert.assertFalse(doneBtnIsEnable);
        Assert.assertNotNull(customerPage.errorMessage());
        Assert.assertTrue(customerPage.errorMessage().isDisplayed());
        Assert.assertEquals(customerPage.errorMessage().getText(), PHONE_NUMBER_ERROR_MESSAGE);
    }

    @Test(priority = 30, groups = "smoke")
    public void addAllInformation() {
        String title = ENTITY_NAME;
        customerPage.createCustomer(title);
        String text = "Text";
        String email = "email@mail.com";
        String number = "2015550123";

        sideBarMenuView.customerBtn().click();
        customerPage.entityTitles().get(0).click();
        customerPage.editPencilBtn().click();
        customerPage.selectCountryEntityView();
        customerPage.descriptionEntityView().sendKeys(text);
        customerPage.cityEntityView().sendKeys(text);
        customerPage.stateEntityView().sendKeys(text);
        customerPage.zipEntityView().sendKeys(text);
        customerPage.addressEntityView().sendKeys(text);
        customerPage.address2EntityView().sendKeys(text);
        customerPage.phoneNumberEntityView().sendKeys(number);
        customerPage.emailEntityView().sendKeys(email);
        customerPage.doneBtnEditView().click();
        customerName = title;

        Assert.assertEquals(customerPage.countrySelectMenuEntityView().getText(), customerPage.getCountry());
        Assert.assertEquals(customerPage.descriptionEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.cityEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.stateEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.zipEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.addressEntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.address2EntityView().getAttribute("value"), text);
        Assert.assertEquals(customerPage.phoneNumberEntityView().getAttribute("value"), "+1" + number);
        Assert.assertEquals(customerPage.emailEntityView().getAttribute("value"), email);
    }
}