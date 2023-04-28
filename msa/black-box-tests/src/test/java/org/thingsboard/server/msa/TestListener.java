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
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.internal.ConstructorOrMethod;

@Slf4j
public class TestListener implements ITestListener {

    public final int MAX_FAILED_TESTS = 10;
    private int failedTestsCount = 0;

    @Override
    public void onTestStart(ITestResult result) {
        log.info("===>>> Test started: " + result.getName());
    }

    /**
     * Invoked when a test succeeds
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        log.info("<<<=== Test completed successfully: " + result.getName());

    }

    /**
     * Invoked when a test fails
     */
    @Override
    public void onTestFailure(ITestResult result) {
        log.info("<<<=== Test failed: " + result.getName());
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return;
        }
        failedTestsCount++;
        if (failedTestsCount >= MAX_FAILED_TESTS) {
            System.setProperty("blackBoxTests.ui.skip", "true");
            log.error("Too many test failures {}. Skipping remaining tests.", failedTestsCount);
        }
    }

    /**
     * Invoked when a test skipped
     */
    @Override
    public void onTestSkipped(ITestResult result) {
        log.info("<<<=== Test skipped: " + result.getName());
    }
}
