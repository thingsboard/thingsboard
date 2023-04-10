package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;

public class AlarmElements extends OtherPageElements{
    public AlarmElements(WebDriver driver) {
        super(driver);
    }

    private static final String ASSIGN_BTN = "//mat-icon[contains(text(),'keyboard_arrow_down')]/parent::button";
    private static final String USER_ASSIGN_DROPDOWN = "//div[@class='user-display-name']/span[contains(text(),'%s')]";
    private static final String ASSIGN_USERS_DISPLAY_NAME = "//div[@class='user-display-name']/span";
    private static final String ASSIGN_USER_DISPLAY_NAME = "//span[@class='user-display-name'][contains(text(),'%s')]";
    private static final String SEARCH_FIELD = "//input[@placeholder='Search users']";

    public WebElement assignBtn() {
        return waitUntilElementToBeClickable(ASSIGN_BTN);
    }

    public WebElement userFromAssignDropDown(String userEmail) {
        return waitUntilElementToBeClickable(String.format(USER_ASSIGN_DROPDOWN, userEmail));
    }

    public WebElement assignedUser(String userEmail) {
        return waitUntilElementToBeClickable(String.format(ASSIGN_USER_DISPLAY_NAME, userEmail));
    }

    public List<WebElement> assignUsers() {
        return waitUntilElementsToBeClickable(ASSIGN_USERS_DISPLAY_NAME);
    }

    public WebElement searchUserField() {
        return waitUntilElementToBeClickable(SEARCH_FIELD);
    }
}
