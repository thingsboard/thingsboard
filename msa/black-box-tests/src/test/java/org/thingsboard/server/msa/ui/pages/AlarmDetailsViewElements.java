package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

public class AlarmDetailsViewElements extends AbstractBasePage {
    public AlarmDetailsViewElements(WebDriver driver) {
        super(driver);
    }

    private static final String ASSIGN_FIELD = "//mat-label[contains(text(),'Assignee')]/parent::label/following-sibling::input";
    private static final String USER_FROM_DROP_DOWN = "//div[@class='user-display-name']/span[text() = '%s']";
    private static final String CLOSE_VIEW_BTN = "//mat-dialog-container//mat-icon[contains(text(),'close')]/parent::button";
    private static final String UNASSIGNED_BTN = "//div[@role='listbox']//mat-icon[text() = 'account_circle']/following-sibling::span";

    public WebElement assignField() {
        return waitUntilElementToBeClickable(ASSIGN_FIELD);
    }

    public WebElement userFromAssignDropdown(String emailOrName) {
        return waitUntilElementToBeClickable(String.format(USER_FROM_DROP_DOWN, emailOrName));
    }

    public WebElement closeViewBtn() {
        return waitUntilElementToBeClickable(CLOSE_VIEW_BTN);
    }

    public WebElement unassignedBtn() {
        return waitUntilElementToBeClickable(UNASSIGNED_BTN);
    }
}
