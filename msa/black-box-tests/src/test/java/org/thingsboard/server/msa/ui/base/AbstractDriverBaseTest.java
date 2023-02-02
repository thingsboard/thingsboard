/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ContainerTestSuite;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.stream.Collectors;

import static org.junit.Assert.fail;
import static org.thingsboard.server.msa.TestProperties.getBaseUiUrl;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

@Slf4j
abstract public class AbstractDriverBaseTest extends AbstractContainerTest {

    protected static WebDriver driver;
    private final Dimension dimension = new Dimension(WIDTH, HEIGHT);
    private static final int WIDTH = 1680;
    private static final int HEIGHT = 1050;
    private static final String REMOTE_WEBDRIVER_HOST = "http://localhost:4444";
    protected static final PageLink pageLink = new PageLink(10);
    private static final ContainerTestSuite instance = ContainerTestSuite.getInstance();
    private JavascriptExecutor js;

    @SneakyThrows
    @BeforeMethod
    public void openBrowser() {
        log.info("===>>> Setup driver");
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        if (instance.isActive()) {
            RemoteWebDriver remoteWebDriver = new RemoteWebDriver(new URL(REMOTE_WEBDRIVER_HOST), options);
            remoteWebDriver.setFileDetector(new LocalFileDetector());
            driver = remoteWebDriver;
        } else {
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        driver.manage().window().setSize(dimension);
        openLocalhost();
    }

    @AfterMethod
    public void closeBrowser() {
        log.info("<<<=== Teardown");
        driver.quit();
    }

    public void openLocalhost() {
        driver.get(getBaseUiUrl());
    }

    public String getUrl() {
        return driver.getCurrentUrl();
    }

    public static WebDriver getDriver() {
        return driver;
    }

    protected boolean urlContains(String urlPath) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(5000));
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            log.error("This URL path is missing: " + urlPath);
            fail("This URL path is missing: " + urlPath);
        }
        return driver.getCurrentUrl().contains(urlPath);
    }

    public void jsClick(WebElement element) {
        js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", element);
    }

    public static RuleChain getRuleChainByName(String name) {
        try {
            return testRestClient.getRuleChains(pageLink).getData().stream()
                    .filter(s -> s.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such rule chain with name: " + name);
            fail("No such rule chain with name: " + name);
            return null;
        }
    }

    public static Customer getCustomerByName(String name) {
        try {
            return testRestClient.getCustomers(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such customer with name: " + name);
            fail("No such customer with name: " + name);
            return null;
        }
    }

    public static DeviceProfile getDeviceProfileByName(String name) {
        try {
            return testRestClient.getDeviceProfiles(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such device profile with name: " + name);
            fail("No such device profile with name: " + name);
            return null;
        }
    }

    public static AssetProfile getAssetProfileByName(String name) {
        try {
            return testRestClient.getAssetProfiles(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such asset profile with name: " + name);
            fail("No such asset profile with name: " + name);
            return null;
        }
    }

    public static void captureScreen(WebDriver driver) {
        Allure.addAttachment("Page screenshot", new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
    }
}