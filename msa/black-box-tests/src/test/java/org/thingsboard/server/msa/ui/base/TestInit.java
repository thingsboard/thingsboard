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

import io.github.bonigarcia.wdm.WebDriverManager;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

@Slf4j
public abstract class TestInit extends Base {

    private final String browser = System.getProperty("blackBoxTests.browser", "chrome");

    protected WebDriver driver;

    Dimension dimension = new Dimension(WIDTH, HEIGHT);

    private static final int WIDTH = 1680;
    private static final int HEIGHT = 1050;
    private static final boolean HEADLESS = true;

    @Before
    public void openBrowser() throws Exception {
        log.info("*----------------------* Setup driver *----------------------*");
        log.info("*----------------------* Starting " + browser + " driver *----------------------*");
        switch (browser) {
            case ("chrome"):
                if (HEADLESS) {
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
                break;

            case ("firefox"):
                if (HEADLESS) {
                    FirefoxOptions options = new FirefoxOptions();
                    options.addArguments("--no-sandbox");
                    options.addArguments("--disable-dev-shm-usage");
                    options.addArguments("--headless");
                    WebDriverManager.firefoxdriver().setup();
                    driver = new FirefoxDriver(options);
                } else {
                    WebDriverManager.firefoxdriver().setup();
                    driver = new ChromeDriver();
                }
                break;
            default:
                throw new Exception("You chose not valid browser!");
        }
        driver.manage().window().setSize(dimension);
    }

    @After
    public void closeBrowser() {
        log.info("*----------------------* Teardown *----------------------*");
        driver.quit();
    }

    public void openUrl(String url) {
        driver.get(url);
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

