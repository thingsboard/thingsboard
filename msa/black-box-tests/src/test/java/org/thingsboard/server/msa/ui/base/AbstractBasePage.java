/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.msa.ui.base;

import lombok.SneakyThrows;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.fail;

abstract public class AbstractBasePage {
    public static final long WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(30);
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected Actions actions;
    protected JavascriptExecutor js;

    public AbstractBasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofMillis(WAIT_TIMEOUT));
        this.actions = new Actions(driver);
        this.js = (JavascriptExecutor) driver;
    }

    @SneakyThrows
    protected static void sleep(double second) {
        Thread.sleep((long) (second * 1000L));
    }

    protected WebElement waitUntilVisibilityOfElementLocated(String locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
        } catch (WebDriverException e) {
            return fail("No visibility element: " + locator);
        }
    }

    protected WebElement waitUntilPresenceOfElementLocated(String locator) {
        try {
            return wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath(locator)));
        } catch (WebDriverException e) {
            return fail("No presence element: " + locator);
        }
    }

    protected WebElement waitUntilElementToBeClickable(String locator) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
        } catch (WebDriverException e) {
            return fail("No clickable element: " + locator);
        }
    }

    protected List<WebElement> waitUntilVisibilityOfElementsLocated(String locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
            return driver.findElements(By.xpath(locator));
        } catch (WebDriverException e) {
            return fail("No visibility elements: " + locator);
        }
    }

    protected List<WebElement> waitUntilElementsToBeClickable(String locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
            return driver.findElements(By.xpath(locator));
        } catch (WebDriverException e) {
            return fail("No clickable elements: " + locator);
        }
    }

    public void waitUntilUrlContainsText(String urlPath) {
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            fail("This URL path is missing");
        }
    }

    protected void moveCursor(WebElement element) {
        actions.moveToElement(element).perform();
    }

    protected void doubleClick(WebElement element) {
        actions.doubleClick(element).build().perform();
    }

    public boolean elementIsNotPresent(String locator) {
        try {
            return wait.until(ExpectedConditions.not(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator))));
        } catch (WebDriverException e) {
            return fail("Element is present: " + locator);
        }
    }

    public boolean elementsIsNotPresent(String locator) {
        try {
            return wait.until(ExpectedConditions.not(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(locator))));
        } catch (WebDriverException e) {
            return fail("Elements is present: " + locator);
        }
    }

    public void waitUntilNumberOfTabToBe(int tabNumber) {
        try {
            wait.until(ExpectedConditions.numberOfWindowsToBe(tabNumber));
        } catch (WebDriverException e) {
            fail("No tabs with this number: " + tabNumber);
        }
    }

    public void jsClick(WebElement element) {
        js.executeScript("arguments[0].click();", element);
    }

    public void enterText(WebElement element, CharSequence keysToEnter) {
        element.click();
        element.sendKeys(keysToEnter);
        if (element.getAttribute("value").isEmpty()) {
            element.sendKeys(keysToEnter);
        }
    }

    public void scrollToElement(WebElement element) {
        js.executeScript("arguments[0].scrollIntoView(true);", element);
    }

    public void waitUntilAttributeContains(WebElement element, String attribute, String value) {
        try {
            wait.until(ExpectedConditions.attributeContains(element, attribute, value));
        } catch (WebDriverException e) {
            fail("Failed to wait until attribute '" + attribute + "' of element '" + element + "' contains value '" + value + "'");
        }
    }

    public void goToNextTab(int tabNumber) {
        waitUntilNumberOfTabToBe(tabNumber);
        ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs.get(tabNumber - 1));
    }

    public static String getRandomNumber() {
        StringBuilder random = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            random.append(ThreadLocalRandom.current().nextInt(0, 100));
        }
        return random.toString();
    }

    public static String randomUUID() {
        UUID randomUUID = UUID.randomUUID();
        return randomUUID.toString().replaceAll("_", "");
    }

    public static String random() {
        return getRandomNumber() + randomUUID().substring(0, 6);
    }

    public static char getRandomSymbol() {
        Random rand = new Random();
        String s = "~`!@#$^&*()_+=-";
        return s.charAt(rand.nextInt(s.length()));
    }

    public void pull(WebElement element, int xOffset, int yOffset) {
        actions.clickAndHold(element).moveByOffset(xOffset, yOffset).release().perform();
    }

    public void waitUntilAttributeToBe(String locator, String attribute, String value) {
        try {
            wait.until(ExpectedConditions.attributeToBe(By.xpath(locator), attribute, value));
        } catch (WebDriverException e) {
            fail("Failed to wait until attribute '" + attribute + "' of element located by '" + locator + "' is '" + value + "'");
        }
    }

    public void clearInputField(WebElement element) {
        element.click();
        element.sendKeys(Keys.CONTROL + "A" + Keys.BACK_SPACE);
    }

    public void waitUntilAttributeToBeNotEmpty(WebElement element, String attribute) {
        try {
            wait.until(ExpectedConditions.attributeToBeNotEmpty(element, attribute));
        } catch (WebDriverException e) {
            fail("Failed to wait until attribute '" + attribute + "' of element '" + element + "' is not empty");
        }
    }
}
