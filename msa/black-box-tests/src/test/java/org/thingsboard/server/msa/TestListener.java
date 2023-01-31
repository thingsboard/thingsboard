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
package org.thingsboard.server.msa;

import io.qameta.allure.listener.TestLifecycleListener;
import io.qameta.allure.model.TestResult;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;

import static org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest.captureScreen;

@Slf4j
public class TestListener implements TestLifecycleListener {

    WebDriver driver;

    @Override
    public void beforeTestStart(TestResult result) {
        log.info("===>>> Test started: " + result.getName());
    }

    @Override
    public void beforeTestStop(TestResult result) {
        driver = AbstractDriverBaseTest.getDriver();
        switch (result.getStatus()) {
            case PASSED:
                log.info("<<<=== Test completed successfully: " + result.getName());
                if (driver != null) {
                    captureScreen(driver);
                }
                break;
            case FAILED:
                log.info("<<<=== Test failed: " + result.getName());
                if (driver != null) {
                    captureScreen(driver);
                }
                break;
            case BROKEN:
                log.info("<<<=== Test broken: " + result.getName());
                if (driver != null) {
                    captureScreen(driver);
                }
                break;
            case SKIPPED:
                log.info("<<<=== Test skipped: " + result.getName());
                if (driver != null) {
                    captureScreen(driver);
                }
                break;
        }
    }
}
