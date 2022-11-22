package org.thingsboard.server.msa.ui.base;

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.thingsboard.server.msa.TestListener;
import org.thingsboard.server.msa.ui.listeners.RetryTestListener;

import java.time.Duration;

@Slf4j
@Listeners({TestListener.class, RetryTestListener.class})
abstract public class DiverBaseTest extends Base {
    protected WebDriver driver;

    private final Dimension dimension = new Dimension(WIDTH, HEIGHT);
    private static final int WIDTH = 1680;
    private static final int HEIGHT = 1050;
    private static final boolean HEADLESS = false;

    @BeforeMethod
    public void openBrowser() {
        log.info("*----------------------* Setup driver *----------------------*");
        if (HEADLESS == true) {
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--headless");
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        } else {
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver();
        }
        driver.manage().window().setSize(dimension);
    }

    @AfterMethod
    public void closeBrowser() {
        log.info("*----------------------* Teardown *----------------------*");
        driver.quit();
    }

    public void openUrl(String url) {
        driver.get(url);
    }

    public String getUrl() {
        return driver.getCurrentUrl();
    }

    public WebDriver getDriver() {
        return driver;
    }

    protected boolean urlContains(String urlPath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(10000));
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            log.error("This URL path is missing");
        }
        return driver.getCurrentUrl().contains(urlPath);
    }
}