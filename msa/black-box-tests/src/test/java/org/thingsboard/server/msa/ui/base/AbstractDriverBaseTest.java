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

import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;
import lombok.extern.slf4j.Slf4j;
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
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.rule.RuleChain;
import org.thingsboard.server.msa.AbstractContainerTest;
import org.thingsboard.server.msa.ContainerTestSuite;

import java.io.ByteArrayInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.thingsboard.server.msa.TestProperties.getBaseUiUrl;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_EMAIL;
import static org.thingsboard.server.msa.ui.utils.Const.TENANT_PASSWORD;

@Slf4j
abstract public class AbstractDriverBaseTest extends AbstractContainerTest {

    protected WebDriver driver;
    private final Dimension dimension = new Dimension(WIDTH, HEIGHT);
    private static final int WIDTH = 1680;
    private static final int HEIGHT = 1050;
    private static final String REMOTE_WEBDRIVER_HOST = "http://localhost:4444";
    protected static final PageLink pageLink = new PageLink(10);
    private static final ContainerTestSuite instance = ContainerTestSuite.getInstance();
    private JavascriptExecutor js;
    public static final long WAIT_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private final Duration duration = Duration.ofMillis(WAIT_TIMEOUT);

    @BeforeClass
    public void startUp() throws MalformedURLException {
        log.info("===>>> Setup driver");
        testRestClient.login(TENANT_EMAIL, TENANT_PASSWORD);
        ChromeOptions options = new ChromeOptions();
        options.setAcceptInsecureCerts(true);
        options.addArguments("-remote-allow-origins=*"); //temporary fix after updating google chrome
        if (instance.isActive()) {
            RemoteWebDriver remoteWebDriver = new RemoteWebDriver(new URL(REMOTE_WEBDRIVER_HOST), options);
            remoteWebDriver.setFileDetector(new LocalFileDetector());
            driver = remoteWebDriver;
        } else {
            WebDriverManager.chromedriver().setup();
            driver = new ChromeDriver(options);
        }
        driver.manage().window().setSize(dimension);
        openBaseUiUrl();
    }

    @BeforeMethod
    public void open() {
        openBaseUiUrl();
    }

    @AfterMethod
    public void addScreenshotToReport() {
        captureScreen(driver, "After test page screenshot");
    }

    @AfterClass
    public void teardown() {
        log.info("<<<=== Teardown");
        driver.quit();
    }

    public String getJwtTokenFromLocalStorage() {
        return (String) getJs().executeScript("return window.localStorage.getItem('jwt_token');");
    }

    public void openBaseUiUrl() {
        driver.get(getBaseUiUrl());
    }

    public String getUrl() {
        return driver.getCurrentUrl();
    }

    public WebDriver getDriver() {
        return driver;
    }

    protected boolean urlContains(String urlPath) {
        WebDriverWait wait = new WebDriverWait(driver, duration);
        try {
            wait.until(ExpectedConditions.urlContains(urlPath));
        } catch (WebDriverException e) {
            return fail("URL not contains " + urlPath);
        }
        return driver.getCurrentUrl().contains(urlPath);
    }

    public void jsClick(WebElement element) {
        getJs().executeScript("arguments[0].click();", element);
    }

    public static RuleChain getRuleChainByName(String name) {
        return testRestClient.getRuleChains(pageLink).getData().stream()
                .filter(s -> s.getName().equals(name))
                .findFirst().orElse(null);
    }

    public List<RuleChain> getRuleChainsByName(String name) {
        return testRestClient.getRuleChains(pageLink).getData().stream()
                .filter(s -> s.getName().equals(name))
                .collect(Collectors.toList());
    }

    public static Customer getCustomerByName(String name) {
        try {
            return testRestClient.getCustomers(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such customer with name: " + name);
            return null;
        }
    }

    public static DeviceProfile getDeviceProfileByName(String name) {
        try {
            return testRestClient.getDeviceProfiles(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such device profile with name: " + name);
            return null;
        }
    }

    public static AssetProfile getAssetProfileByName(String name) {
        try {
            return testRestClient.getAssetProfiles(pageLink).getData().stream()
                    .filter(x -> x.getName().equals(name)).collect(Collectors.toList()).get(0);
        } catch (Exception e) {
            log.error("No such asset profile with name: " + name);
            return null;
        }
    }

    public void captureScreen(WebDriver driver, String screenshotName) {
        if (driver instanceof TakesScreenshot) {
            Allure.addAttachment(screenshotName,
                    new ByteArrayInputStream(((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES)));
        }
    }

    public JavascriptExecutor getJs() {
        return js = (JavascriptExecutor) driver;
    }

    public void assertIsDisplayed(WebElement element) {
        assertThat(element.isDisplayed()).as(element + " is displayed").isTrue();
    }

    public void assertIsDisable(WebElement element) {
        assertThat(element.isEnabled()).as(element + " is disabled").isFalse();
    }

    public void deleteRuleChainByName(String ruleChainName) {
        List<RuleChain> ruleChains = getRuleChainsByName(ruleChainName);
        if (!ruleChains.isEmpty()) {
            ruleChains.forEach(rc -> testRestClient.deleteRuleChain(rc.getId()));
        }
    }

    public void setRootRuleChain(String ruleChainName) {
        List<RuleChain> ruleChains = getRuleChainsByName(ruleChainName);
        if (!ruleChains.isEmpty()) {
            testRestClient.setRootRuleChain(ruleChains.stream().findFirst().get().getId());
        }
    }
}
