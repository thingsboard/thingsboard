package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class AlarmHelper extends AlarmElements {
    public AlarmHelper(WebDriver driver) {
        super(driver);
    }

    public void assignTo(String user) {
        jsClick(assignBtn());
        userFromAssignDropDown(user).click();
    }
}
