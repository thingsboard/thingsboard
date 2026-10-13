// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.HashSet;
import java.util.Set;

public class SolutionTemplatesInstalledViewHelper extends SolutionTemplatesInstalledViewElements {
    public SolutionTemplatesInstalledViewHelper(WebDriver driver) {
        super(driver);
    }

    public void waitUntilInstallFinish() {
        waitUntilInvisibilityOfElementLocated(solutionTemplateInstallProgressPopUp());
    }

    private String firstUserName;
    private String secondUserName;

    public void setFirstUserName() {
        this.firstUserName = user1().getText();
    }

    public void setSecondUserName() {
        this.secondUserName = user2().getText();
    }

    public String getFirstUserName() {
        return this.firstUserName;
    }

    public String getSecondUserName() {
        return this.secondUserName;
    }

    private String getLink(String stepName, WebElement element) {
        return Allure.step(stepName, () -> {
            element.click();
            goToNextTab(2);
            captureScreen(driver, stepName);
            String url = driver.getCurrentUrl();
            driver.close();
            goToNextTab(1);
            return url;
        });
    }

    public Set<String> getDashboardLinks() {
        Set<String> urls = new HashSet<>();
        return Allure.step("Check redirect on main dashboard page", () -> {
            linkDashboardBtn().forEach(linkBtn -> {
                int number = 1;
                linkBtn.click();
                goToNextTab(2);
                captureScreen(driver, "Check redirect on main dashboard page #" + number);
                urls.add(driver.getCurrentUrl());
                driver.close();
                goToNextTab(1);
                number++;
            });
            return urls;
        });
    }

    public String getGuideLink() {
        return getLink("Check redirect on dashboard development guide page", linkGuideBtn());
    }

    public String getHttpApiLink() {
        return getLink("Check redirect on HTTP API documentation page", linkHttpApiBtn());
    }

    public String getConnectionDevicesLink() {
        return getLink("Check redirect on connection device documentation page", linkConnectionDevices());
    }

    public String getAlarmRuleLink() {
        return getLink("Check redirect on alarm rule documentation page", linkAlarmRuleBtn());
    }

    public String getDeviceProfileLink() {
        return getLink("Check redirect on device profile page", linkDeviceProfileBtn());
    }

    public String getAlarmRulesLink() {
        return getLink("Check redirect on device profile page", linkAlarmRulesBtn());
    }

    public String getThingsBoardIoTGatewayLink() {
        return getLink("Check redirect on ThingsBoard IoT Gateway documentation page", linkThingsBoardIoTGateway());
    }

    public String getThingsBoardMQTTGatewayLink() {
        return getLink("Check redirect on ThingsBoard IoT Gateway documentation page", linkThingsBoardMQTTGateway());
    }

    public String getThingsBoardIntegration() {
        return getLink("Check redirect on ThingsBoard Integration documentation page", linkIntegration());
    }

    public void goToMainDashboard() {
        solutionTemplateInstalledPopUp();
        jsClick(goToMainDashboardPageBtn());
        waitUntilUrlContainsText("dashboards");
    }
}