// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class OtherPageElementsHelper extends OtherPageElements {
    public OtherPageElementsHelper(WebDriver driver) {
        super(driver);
    }

    private String headerName;

    public void setHeaderName() {
        this.headerName = headerNameView().getText();
    }

    public String getHeaderName() {
        return headerName;
    }

    public boolean assertEntityIsNotPresent(String entityName) {
        return elementIsNotPresent(getEntity(entityName));
    }

    public void goToHelpPage() {
        helpBtn().click();
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
}

