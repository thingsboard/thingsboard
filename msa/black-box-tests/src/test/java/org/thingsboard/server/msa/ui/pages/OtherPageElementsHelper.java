package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;

public class OtherPageElementsHelperAbstract extends OtherPageElementsAbstract {
    public OtherPageElementsHelperAbstract(WebDriver driver) {
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

