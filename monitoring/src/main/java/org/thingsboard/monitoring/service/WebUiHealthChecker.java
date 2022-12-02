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
package org.thingsboard.monitoring.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.thingsboard.monitoring.data.Latencies;
import org.thingsboard.monitoring.data.MonitoredServiceKey;
import org.thingsboard.monitoring.util.TbStopWatch;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.openqa.selenium.By.xpath;
import static org.openqa.selenium.support.ui.ExpectedConditions.elementToBeClickable;
import static org.openqa.selenium.support.ui.ExpectedConditions.urlContains;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "monitoring.ui.enabled", havingValue = "true")
public class WebUiHealthChecker {

    @Value("${monitoring.ui.url}")
    private String url;
    @Value("${monitoring.auth.username}")
    private String username;
    @Value("${monitoring.auth.password}")
    private String password;
    @Value("${monitoring.ui.monitoring_rate_sec}")
    private int monitoringRateSec;
    @Value("${monitoring.rest_request_timeout_ms}")
    private int timeoutMs;
    @Value("${monitoring.ui.webdriver_location}")
    private File webdriverLocation;

    private final MonitoringReporter monitoringReporter;
    private final ScheduledExecutorService monitoringExecutor;
    private final TbStopWatch stopWatch;

    private static final String EMAIL_FIELD = "//input[@id='username-input']";
    private static final String PASSWORD_FIELD = "//input[@id='password-input']";
    private static final String SUBMIT_BTN = "//button[@type='submit']";
    private static final String DEVICES_BTN = "//mat-toolbar//a[@href='/devices']";

    @PostConstruct
    private void init() {
        System.setProperty("webdriver.gecko.driver", webdriverLocation.getAbsolutePath());
        System.setProperty(FirefoxDriver.SystemProperty.BROWSER_LOGFILE, "/dev/null");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void startMonitoring() {
        monitoringExecutor.scheduleWithFixedDelay(() -> {
            WebDriver driver = null;
            try {
                FirefoxOptions options = new FirefoxOptions();
                options.setHeadless(true);
                driver = new FirefoxDriver(options);
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(timeoutMs));
                driver.manage().window().maximize();
                driver.get(url + "/login");

                try {
                    stopWatch.start();
                    wait.until(elementToBeClickable(xpath(EMAIL_FIELD))).sendKeys(username);
                    wait.until(elementToBeClickable(xpath(PASSWORD_FIELD))).sendKeys(password);
                    wait.until(elementToBeClickable(xpath(SUBMIT_BTN))).click();
                    monitoringReporter.reportLatency(Latencies.WEB_UI_LOAD, stopWatch.getTime());

                    wait.until(urlContains("/home"));
                    wait.until(elementToBeClickable(xpath(DEVICES_BTN))).click();
                } catch (Exception e) {
                    throw new RuntimeException("Expected web UI elements were not displayed", e);
                }

                monitoringReporter.serviceIsOk(MonitoredServiceKey.WEB_UI);
            } catch (Exception e) {
                monitoringReporter.serviceFailure(MonitoredServiceKey.WEB_UI, e);
            } finally {
                if (driver != null) driver.quit();
            }
        }, 0, monitoringRateSec, TimeUnit.SECONDS);
    }

}
