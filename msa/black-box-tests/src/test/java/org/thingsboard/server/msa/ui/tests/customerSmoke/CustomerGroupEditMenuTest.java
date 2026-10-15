// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.tests.customerSmoke;

import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;
import org.thingsboard.server.msa.ui.pages.CustomerPageHelper;
import org.thingsboard.server.msa.ui.pages.LoginPageHelper;
import org.thingsboard.server.msa.ui.pages.SideBarMenuViewHelper;
import org.thingsboard.server.msa.ui.utils.DataProviderCredential;
import org.thingsboard.server.msa.ui.utils.EntityPrototypes;

import static org.thingsboard.server.msa.ui.base.AbstractBasePage.getRandomNumber;
import static org.thingsboard.server.msa.ui.base.AbstractBasePage.random;
import static org.thingsboard.server.msa.ui.utils.Const.EMPTY_GROUP_NAME_MESSAGE;
import static org.thingsboard.server.msa.ui.utils.Const.ENTITY_NAME;

public class CustomerGroupEditMenuTest extends AbstractDriverBaseTest {
    private SideBarMenuViewHelper sideBarMenuView;
    private CustomerPageHelper customerPage;
    private String customerGroupName;

    @BeforeClass
    public void login() {
        new LoginPageHelper(driver).authorizationTenant();
        sideBarMenuView = new SideBarMenuViewHelper(driver);
        customerPage = new CustomerPageHelper(driver);
    }

    @AfterMethod
    public void delete() {
        if (customerGroupName != null) {
            testRestClient.deleteEntityGroup(getEntityGroupByName(EntityType.CUSTOMER, customerGroupName).getId());
            customerGroupName = null;
        }
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer group")
    @Test(priority = 10, groups = "smoke")
    @Description("Change name by edit menu")
    public void changeTitle() {
        String customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));
        this.customerGroupName = customerGroupName;
        String changedName = "Changed" + getRandomNumber();

        sideBarMenuView.goToCustomerGroups();
        customerPage.detailsBtn(customerGroupName).click();
        customerPage.setHeaderName();
        String nameBefore = customerPage.getHeaderName();
        customerPage.entityGroupEditPencilBtn().click();
        customerPage.changeNameEditMenu(changedName);
        customerPage.entityGroupDoneBtnEditView().click();
        this.customerGroupName = changedName;
        customerPage.setHeaderName();
        String nameAfter = customerPage.getHeaderName();

        Assert.assertNotEquals(nameBefore, nameAfter);
        Assert.assertEquals(changedName, nameAfter);
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer group")
    @Test(priority = 20, groups = "smoke")
    @Description("Delete name and save")
    public void deleteName() {
        String customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));
        this.customerGroupName = customerGroupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.detailsBtn(customerGroupName).click();
        customerPage.entityGroupEditPencilBtn().click();
        customerPage.nameFieldEditMenu().clear();

        Assert.assertFalse(customerPage.entityGroupDoneBtnVisibleEditView().isEnabled());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer group")
    @Test(priority = 20, groups = "smoke")
    @Description("Save only with space")
    public void saveOnlyWithSpace() {
        String customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER));
        this.customerGroupName = customerGroupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.detailsBtn(customerGroupName).click();
        customerPage.entityGroupEditPencilBtn().click();
        customerPage.changeNameEditMenu(" ");
        customerPage.entityGroupDoneBtnEditView().click();
        customerPage.setHeaderName();

        Assert.assertNotNull(customerPage.warningMessage());
        Assert.assertTrue(customerPage.warningMessage().isDisplayed());
        Assert.assertEquals(customerPage.warningMessage().getText(), EMPTY_GROUP_NAME_MESSAGE);
        Assert.assertEquals(customerGroupName, customerPage.getHeaderName());
    }

    @Epic("Customers smoke tests")
    @Feature("Edit customer group")
    @Test(priority = 20, groups = "smoke", dataProviderClass = DataProviderCredential.class, dataProvider = "editMenuDescription")
    @Description("Write the description and save the changes/Change the description and save the changes/Delete the description and save the changes")
    public void editDescription(String description, String newDescription, String finalDescription) {
        String customerGroupName = ENTITY_NAME + random();
        testRestClient.postEntityGroup(EntityPrototypes.defaultEntityGroupPrototype(customerGroupName, EntityType.CUSTOMER, description));
        this.customerGroupName = customerGroupName;

        sideBarMenuView.goToCustomerGroups();
        customerPage.detailsBtn(customerGroupName).click();
        customerPage.entityGroupEditPencilBtn().click();
        customerPage.descriptionEntityView().sendKeys(newDescription);
        customerPage.entityGroupDoneBtnEditView().click();
        customerPage.setDescription();

        Assert.assertEquals(customerPage.getDescription(), finalDescription);
    }
}
