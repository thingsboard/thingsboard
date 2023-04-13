package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class AlarmDetailsViewHelper extends AlarmDetailsViewElements{
    public AlarmDetailsViewHelper(WebDriver driver) {
        super(driver);
    }

    public void assignAlarmTo(String emailOrName) {
        assignField().click();
        userFromAssignDropdown(emailOrName).click();
    }

    public void unassignedAlarm() {
        assignField().click();
        unassignedBtn().click();
    }
}
