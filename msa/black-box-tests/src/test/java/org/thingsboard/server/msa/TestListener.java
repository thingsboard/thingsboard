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
package org.thingsboard.server.msa;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.SkipException;
import org.testng.internal.ConstructorOrMethod;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;

@Slf4j
public class TestListener implements ITestListener {

    int failedTestsCount = 0;
    private static final int DEFAULT_COUNT_FOR_SKIP = 5;

    @Override
    public void onTestStart(ITestResult result) {
        log.info("===>>> Test started: {}", result.getName());
    }

    /**
     * Invoked when a test succeeds
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<<<=== Test completed successfully: {}", result.getName());
    }

    /**
     * Invoked when a test fails
     */
    @Override
    public void onTestFailure(ITestResult result) {
        log.info("<<<=== Test failed: {}", result.getName());
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return;
        }
        failedTestsCount++;
        int countForSkip = getCountForSkip();
        if (failedTestsCount >= countForSkip) {
            closeWebDriver(result);
            throw new SkipException(String.format("Too many test failures (%d). Skipping remaining tests.", countForSkip));
        }
    }

    /**
     * Invoked when a test skipped
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        log.info("<<<=== Test skipped: {}", result.getName());
    }

    private int getCountForSkip() {
        String countForSkipProperty = System.getProperty("countForSkip");
        if (countForSkipProperty != null) {
            return Integer.parseInt(countForSkipProperty);
        }
        return DEFAULT_COUNT_FOR_SKIP;
    }

    private void closeWebDriver(ITestResult result) {
        Object instance = result.getInstance();
        if (instance instanceof AbstractDriverBaseTest) {
            WebDriver driver = ((AbstractDriverBaseTest) instance).getDriver();
            driver.close();
        } else {
            log.warn("Unable to close WebDriver. AbstractDriverBaseTest instance not found.");
        }
    }
}
