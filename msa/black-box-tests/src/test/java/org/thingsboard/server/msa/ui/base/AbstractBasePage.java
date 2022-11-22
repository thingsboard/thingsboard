package org.thingsboard.server.msa.ui.base;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.thingsboard.rest.client.RestClient;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.msa.ui.utils.Const;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
abstract public class BasePage extends Base {
    protected WebDriver driver;
    protected WebDriverWait wait;
    protected Actions actions;
    protected RestClient client;
    protected PageLink pageLink;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofMillis(5000));
        this.actions = new Actions(driver);
        try {
            client = new RestClient(Const.URL);
            client.login(Const.TENANT_EMAIL, Const.TENANT_PASSWORD);
            pageLink = new PageLink(10);
        } catch (Exception e) {
            log.info("Can't login");
        }
    }

    @SneakyThrows
    protected static void sleep(double second) {
        Thread.sleep((long) (second * 1000L));
    }

    protected WebElement waitUntilVisibilityOfElementLocated(String locator) {
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
        } catch (WebDriverException e) {
            log.error("No visibility element: " + locator);
            return null;
        }
    }

    protected WebElement waitUntilElementToBeClickable(String locator) {
        try {
            return wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
        } catch (WebDriverException e) {
            log.error("No clickable element: " + locator);
            return null;
        }
    }

    protected List<WebElement> waitUntilVisibilityOfElementsLocated(String locator) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(locator)));
            return driver.findElements(By.xpath(locator));
        } catch (WebDriverException e) {
            log.error("No visibility elements: " + locator);
            return null;
        }
    }

    protected List<WebElement> waitUntilElementsToBeClickable(String locator) {
        try {
            wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
            return driver.findElements(By.xpath(locator));
        } catch (WebDriverException e) {
            log.error("No clickable elements: " + locator);
            return null;
        }
    }

    public void waitUntilUrlContainsText(String urlPath) {
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            log.error("This URL path is missing");
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
            throw new AssertionError("Element is present");
        }
    }

    public boolean elementsIsNotPresent(String locator) {
        try {
            return wait.until(ExpectedConditions.not(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(locator))));
        } catch (WebDriverException e) {
            throw new AssertionError("Elements is present");
        }
    }

    public void waitUntilNumberOfTabToBe(int tabNumber) {
        try {
            wait.until(ExpectedConditions.numberOfWindowsToBe(tabNumber));
        } catch (WebDriverException e) {
            log.error("No tabs with this number");
        }
    }

    public void goToNextTab(int tabNumber) {
        waitUntilNumberOfTabToBe(tabNumber);
        ArrayList<String> tabs = new ArrayList<>(driver.getWindowHandles());
        driver.switchTo().window(tabs.get(tabNumber - 1));
    }
}
