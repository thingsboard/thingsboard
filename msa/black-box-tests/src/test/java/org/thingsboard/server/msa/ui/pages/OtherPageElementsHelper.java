/**
 * Copyright © 2016-2022 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
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

    public boolean entityIsNotPresent(String entityName) {
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

    public void changeNameEditMenu(String newName) {
        nameFieldEditMenu().sendKeys(Keys.CONTROL + "a" + Keys.BACK_SPACE);
        nameFieldEditMenu().sendKeys(newName);
    }

    public void changeDescription(String newDescription) {
        descriptionEntityView().sendKeys(Keys.CONTROL + "a" + Keys.BACK_SPACE);
        descriptionEntityView().sendKeys(newDescription);
    }

    public String deleteRuleChainTrash(String entityName) {
        String s = "";
        if (deleteBtn(entityName) != null) {
            deleteBtn(entityName).click();
            warningPopUpYesBtn().click();
            return entityName;
        } else {
            for (int i = 0; i < deleteBtns().size(); i++) {
                if (deleteBtns().get(i).isEnabled()) {
                    deleteBtns().get(i).click();
                    warningPopUpYesBtn().click();
                    if (elementIsNotPresent(getWarningMessage())) {
                        s = driver.findElements(By.xpath(getDeleteBtns()
                                + "/../../../mat-cell/following-sibling::mat-cell/following-sibling::mat-cell[contains(@class,'cdk-column-name')]/span")).get(i).getText();
                        break;
                    }
                }
            }
            return s;
        }
    }

    public String deleteSelected(String entityName) {
        String s = "";
        if (deleteBtn(entityName) != null) {
            checkBox(entityName).click();
            deleteSelectedBtn().click();
            warningPopUpYesBtn().click();
            return entityName;
        } else {
            for (int i = 0; i < checkBoxes().size(); i++) {
                if (checkBoxes().get(i).isDisplayed()) {
                    s = driver.findElements(By.xpath(getCheckboxes() + "/../../mat-cell/following-sibling::mat-cell/following-sibling::mat-cell[contains(@class,'cdk-column-name')]/span")).get(i).getText();
                    checkBox(s).click();
                    deleteSelectedBtn().click();
                    warningPopUpYesBtn().click();
                    if (elementIsNotPresent(getWarningMessage())) {
                        break;
                    }
                }
            }
            return s;
        }
    }

    public void searchEntity(String namePath) {
        searchBtn().click();
        searchField().sendKeys(namePath);
        sleep(0.5);
    }
}

