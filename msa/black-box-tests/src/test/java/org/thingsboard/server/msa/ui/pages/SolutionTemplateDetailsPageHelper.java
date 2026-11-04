// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import io.qameta.allure.Allure;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import static org.assertj.core.api.Fail.fail;

public class SolutionTemplateDetailsPageHelper extends SolutionTemplateDetailsPageElements {
    public SolutionTemplateDetailsPageHelper(WebDriver driver) {
        super(driver);
    }

    private String headOfTitleCardName;
    private String titleCardParagraphText;
    private String solutionDescriptionParagraphText;

    public void setHeadOfTitleName() {
        headOfTitleCardName = headOfTitleCard().getText();
    }

    public void setTitleCardParagraphText() {
        titleCardParagraphText = titleCardParagraph().getText();
    }

    public void setSolutionDescriptionParagraphText() {
        solutionDescriptionParagraphText = solutionDescriptionParagraph().getText();
    }

    public String getHeadOfTitleCardName() {
        return headOfTitleCardName;
    }

    public String getTitleCardParagraphText() {
        return titleCardParagraphText;
    }

    public String getSolutionDescriptionParagraphText() {
        return solutionDescriptionParagraphText;
    }

    private void checkScreenshotContent(WebElement screenshot, String urlScreenshotPath) {
        if (!waitUntilVisibilityOfElementLocated(screenshot).isDisplayed()) {
            fail("Screenshot at point " + screenshots().indexOf(screenshot) + " isn't displayed");
            if (!screenshot.getCssValue("background-image").contains(urlScreenshotPath)) {
                fail("Screenshot at point " + screenshots().indexOf(screenshot) + " isn't " + urlScreenshotPath.split("/")[1]);
            }
        }
    }

    private boolean screenshotsAreCorrected(String urlScreenshotPath) {
        sleep(3); //wait until images completely load on page
        for (WebElement screenshot : screenshots()) {
            int imageNumber = screenshots().indexOf(screenshot);
            checkScreenshotContent(screenshot, urlScreenshotPath);
            Allure.step("Check screenshot at point " + imageNumber, () ->
                    captureScreen(driver, "Screenshot #" + imageNumber));
            if (imageNumber != screenshotCircles().size() - 1) {
                swipeScreenshotRightBtn().click();
                waitUntilInvisibilityOfElementLocated(screenshot);
            }
        }
        return true;
    }

    public boolean assertTemperatureHumiditySensorsScreenshotsAreCorrected() {
        return screenshotsAreCorrected("temperature_sensors/temperature-sensors");
    }

    public boolean assertSmartOfficeScreenshotsAreCorrected() {
        return screenshotsAreCorrected("smart_office/smart-office");
    }

    public boolean assertFleetTrackingScreenshotsAreCorrected() {
        return screenshotsAreCorrected("fleet_tracking/fleet-tracking");
    }

    public boolean assertAirQualityMonitoringScreenshotsAreCorrected() {
        return screenshotsAreCorrected("air_quality_index/air-quality-index");
    }

    public boolean assertWaterMeteringScreenshotsAreCorrected() {
        return screenshotsAreCorrected("water_metering/water-metering");
    }

    public boolean assertSmartRetailScreenshotsAreCorrected() {
        return screenshotsAreCorrected("smart_retail/smart-retail");
    }

    public boolean assertSmartIrrigationScreenshotsAreCorrected() {
        return screenshotsAreCorrected("smart_irrigation/smart-irrigation");
    }

    public boolean assignedLivingScreenshotsAreCorrected() {
         return screenshotsAreCorrected("assisted_living/assisted-living");
    }
}

