package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;

public class DevicePageHelper extends DevicePageElements{
    public DevicePageHelper(WebDriver driver) {
        super(driver);
    }

    public void openDeviceAlarms(String deviceName) {
        device(deviceName).click();
        deviceDetailsAlarmsBtn().click();
    }
}
