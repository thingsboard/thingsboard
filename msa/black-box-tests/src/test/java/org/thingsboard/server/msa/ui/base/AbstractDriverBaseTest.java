/**
 * Copyright © 2016-2022 The Thingsboard Authors
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

import com.google.common.io.Files;
import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Attachment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Listeners;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ui.listeners.TestListener;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.stream.Collectors;

@Slf4j
@Listeners(TestListener.class)
abstract public class AbstractDriverBaseTest extends AbstractContainerTest {

    protected WebDriver driver;
    private final Dimension dimension = new Dimension(WIDTH, HEIGHT);
    private static final int WIDTH = 1680;
    private static final int HEIGHT = 1050;
    private static final boolean HEADLESS = false;
    protected static final PageLink pageLink = new PageLink(10);

    @BeforeSuite
    public void beforeUISuite() {
    }

    @BeforeMethod
    public void openBrowser() throws MalformedURLException {
        log.info("*----------------------* Setup driver *----------------------*");
        ChromeOptions options = new ChromeOptions();
        options.setHeadless(HEADLESS);
        options.setAcceptInsecureCerts(true);

        // requirement:
        // docker run --name=chrome --rm --network=host -p 4444:4444 -p 7900:7900 --shm-size="2g" -e SE_NODE_MAX_SESSIONS=8 -e SE_NODE_OVERRIDE_MAX_SESSIONS=true -e SE_NODE_SESSION_TIMEOUT=90 -e SE_SCREEN_WIDTH=1920 -e SE_SCREEN_HEIGHT=1080 -e SE_SCREEN_DEPTH=24 -e SE_SCREEN_DPI=74 selenium/standalone-chrome
        RemoteWebDriver remoteWebDriver = new RemoteWebDriver(new URL("http://localhost:4444"), options);
        remoteWebDriver.setFileDetector(new LocalFileDetector());

        driver = remoteWebDriver;
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

    public boolean getHeadless() {
        return HEADLESS;
    }

    protected boolean urlContains(String urlPath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(5000));
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            log.error("This URL path is missing");
        }
        return driver.getCurrentUrl().contains(urlPath);
    }

    public static RuleChain getRuleChainByName(String name) {
        return testRestClient.getRuleChains(pageLink).getData().stream()
                .filter(s -> s.getName().equals(name)).collect(Collectors.toList()).get(0);
    }

    public static Customer getCustomerByName(String name) {
        return testRestClient.getCustomers(pageLink).getData().stream()
                .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
    }

    @SneakyThrows
    @Attachment(value = "Page screenshot", type = "image/png")
    public static byte[] captureScreen(WebDriver driver, String dirPath) {
        File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        FileUtils.copyFile(screenshot, new File("./target/allure-results/screenshots/" + dirPath + "//" + screenshot.getName()));
        return Files.toByteArray(screenshot);
    }
}