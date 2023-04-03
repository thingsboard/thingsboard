package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DevicePageElements extends OtherPageElements{
    public DevicePageElements(WebDriver driver) {
        super(driver);
    }

    private static final String DEVICE = "//table//span[text()='%s']";
    private static final String DEVICE_DETAILS_ALARMS = "//tb-details-panel//span[text()='Alarms']";

    public WebElement device(String deviceName) {
        return waitUntilElementToBeClickable(String.format(DEVICE, deviceName));
    }

    public WebElement deviceDetailsAlarmsBtn() {
        return waitUntilElementToBeClickable(DEVICE_DETAILS_ALARMS);
    }
}
