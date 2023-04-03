package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AlarmElements extends OtherPageElements{
    public AlarmElements(WebDriver driver) {
        super(driver);
    }

    private static final String ASSIGN_BTN = "//mat-icon[contains(text(),'keyboard_arrow_down')]/parent::button";
    private static final String USER_ASSIGN_DROPDOWN = "//div[contains(@class,'tb-assignee')]//span[contains(text(),'%s')]";
    private static final String ASSIGN_USER_DISPLAY_NAME = "//span[text()='%s']/ancestor::mat-row//span[@class='user-display-name']";

    public WebElement assignBtn() {
        return waitUntilElementToBeClickable(ASSIGN_BTN);
    }

    public WebElement userFromAssignDropDown(String userEmail) {
        return waitUntilElementToBeClickable(String.format(USER_ASSIGN_DROPDOWN, userEmail));
    }

    public WebElement assignUserDisplayName(String userEmail) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_USER_DISPLAY_NAME, userEmail));
    }
}
