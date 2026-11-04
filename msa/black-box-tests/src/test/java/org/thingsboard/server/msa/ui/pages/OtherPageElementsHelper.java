// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class OtherPageElementsHelper extends OtherPageElements {
    public OtherPageElementsHelper(WebDriver driver) {
        super(driver);
    }

    private String headerName;
    private String entityGroupName;

    public void setHeaderName() {
        this.headerName = headerNameView().getText();
    }

    public void setEntityGroupName() {
        this.entityGroupName = entityGroups().get(0).getText();
    }

    public void setEntityGroupName(int i) {
        this.entityGroupName = entityGroups().get(i).getText();
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getEntityGroupName() {
        return entityGroupName;
    }

    public boolean assertEntityIsNotPresent(String entityName) {
        return elementIsNotPresent(getEntity(entityName));
    }

    private void clickHelpButton(WebElement helpBtn) {
        helpBtn.click();
        try {
            wait.until(ExpectedConditions.numberOfWindowsToBe(2));
        } catch (WebDriverException e) {
            helpBtn.click();
        }
    }

    public void goToHelpPage() {
        clickHelpButton(helpBtn());
        goToNextTab(2);
    }

    public void goToHelpEntityGroupPage() {
        clickHelpButton(helpBtnEntityGroup());
        goToNextTab(2);
    }

    public void clickOnCheckBoxes(int count) {
        for (int i = 0; i < count; i++) {
            checkBoxes().get(i).click();
        }
    }

    public void changeNameEditMenu(CharSequence keysToSend) {
        nameFieldEditMenu().click();
        nameFieldEditMenu().clear();
        nameFieldEditMenu().sendKeys(keysToSend);
    }

    public void changeDescription(String newDescription) {
        descriptionEntityView().click();
        descriptionEntityView().clear();
        descriptionEntityView().sendKeys(newDescription);
    }

    public String deleteRuleChainTrash(String entityName) {
        deleteBtn(entityName).click();
        warningPopUpYesBtn().click();
        return entityName;
    }

    public String deleteSelected(String entityName) {
        checkBox(entityName).click();
        jsClick(deleteSelectedBtn());
        warningPopUpYesBtn().click();
        return entityName;
    }

    public void deleteSelected(int countOfCheckBoxes) {
        clickOnCheckBoxes(countOfCheckBoxes);
        jsClick(deleteSelectedBtn());
        warningPopUpYesBtn().click();
    }

    public void searchEntity(String namePath) {
        searchBtn().click();
        searchField().sendKeys(namePath);
        sleep(0.5);
    }

    public void doubleClickOnEntityGroup(String entityGroupName) {
        doubleClick(entity(entityGroupName));
    }

    public void sortByNameDown() {
        doubleClick(sortByNameBtn());
    }

    public void changeOwner(String customerName) {
        changeOwnerViewField().click();
        entityFromDropDown(customerName).click();
        changeOwnerViewChangeOwnerBtn().click();
        warningPopUpYesBtn().click();
    }

    public void changeItemsCountPerPage(int itemCount) {
        itemsPerPage().click();
        WebElement element = itemsCount(itemCount);
        element.click();
        waitUntilInvisibilityOfElementLocated(element);
    }
}
