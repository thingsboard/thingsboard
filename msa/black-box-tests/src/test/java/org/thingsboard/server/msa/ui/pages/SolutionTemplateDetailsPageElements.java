// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.msa.ui.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.thingsboard.server.msa.ui.base.AbstractBasePage;

import java.util.List;

public class SolutionTemplateDetailsPageElements extends AbstractBasePage {
    public SolutionTemplateDetailsPageElements(WebDriver driver) {
        super(driver);
    }

    private static final String HEAD_OF_TITLE_CARD = "//h2";
    private static final String TITLE_CARD_PARAGRAPH = "//div[contains(@class,'title-container')]//p[1]";
    private static final String SOLUTION_DESCRIPTION_PARAGRAPH = "//h3/../p[1]";
    private static final String SCREENSHOT_CIRCLES = "//ul/li/div";
    private static final String SCREENSHOTS = "//ngx-hm-carousel//section/article[@ngx-hm-carousel-item]/div";
    private static final String SWIPE_SCREENSHOT_RIGHT_BTN = "//mat-icon[text()='keyboard_arrow_right']//ancestor::button";
    private static final String INSTALL_BTN = "//span[contains(text(),'Install')]/parent::button";
    private static final String DELETE_BTN = "//span[contains(text(),'Delete')]/parent::button";
    private static final String INSTRUCTION_BTN = "//div[@fxlayoutalign='end center']/button[@color='primary']";

    public WebElement headOfTitleCard() {
        return waitUntilVisibilityOfElementLocated(HEAD_OF_TITLE_CARD);
    }

    public WebElement titleCardParagraph() {
        return waitUntilVisibilityOfElementLocated(TITLE_CARD_PARAGRAPH);
    }

    public WebElement solutionDescriptionParagraph() {
        return waitUntilVisibilityOfElementLocated(SOLUTION_DESCRIPTION_PARAGRAPH);
    }

    public List<WebElement> screenshotCircles() {
        return waitUntilElementsToBeClickable(SCREENSHOT_CIRCLES);
    }

    public List<WebElement> screenshots() {
        return waitUntilPresenceOfElementsLocated(SCREENSHOTS);
    }

    public WebElement swipeScreenshotRightBtn() {
        return waitUntilElementToBeClickable(SWIPE_SCREENSHOT_RIGHT_BTN);
    }

    public WebElement installBtn() {
        return waitUntilElementToBeClickable(INSTALL_BTN);
    }

    public WebElement deleteBtn() {
        return waitUntilElementToBeClickable(DELETE_BTN);
    }

    public WebElement instructionBtn() {
        return waitUntilElementToBeClickable(INSTRUCTION_BTN);
    }
}
