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
package org.thingsboard.server.msa.ui.listeners;


import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest;

import static org.thingsboard.server.msa.ui.base.AbstractDriverBaseTest.captureScreen;

@Slf4j
public class TestListener implements ITestListener {

    public void onTestSuccess(ITestResult tr) {
        String str = "Test " + tr.getMethod().getMethodName() + " success";
        log.info("*----------------------* " + str + " *----------------------*");
        if (!((AbstractDriverBaseTest) tr.getInstance()).isHeadless()) {
            WebDriver driver = ((AbstractDriverBaseTest) tr.getInstance()).getDriver();
            captureScreen(driver, "success");
        }
    }

    public void onTestFailure(ITestResult tr) {
        String str = "Test " + tr.getMethod().getMethodName() + " failure";
        String str1 = "Failed because of - " + tr.getThrowable();
        log.info("*----------------------* " + str + " *----------------------*");
        log.info("*----------------------* " + str1 + " *----------------------*");
        if (!((AbstractDriverBaseTest) tr.getInstance()).isHeadless()) {
            WebDriver driver = ((AbstractDriverBaseTest) tr.getInstance()).getDriver();
            captureScreen(driver, "failure");
        }
    }

    public void onTestSkipped(ITestResult tr) {
        String str = "Test " + tr.getMethod().getMethodName() + " skipped";
        String str1 = "Skipped because of - " + tr.getThrowable();
        log.info("*----------------------* " + str + " *----------------------*");
        log.info("*----------------------* " + str1 + " *----------------------*");
        if (!((AbstractDriverBaseTest) tr.getInstance()).isHeadless()) {
            WebDriver driver = ((AbstractDriverBaseTest) tr.getInstance()).getDriver();
            captureScreen(driver, "skipped");
        }
    }

    public void onStart(ITestContext testContext) {
        String str = "Test " + testContext.getCurrentXmlTest().getName() + " start";
        log.info("*----------------------* " + str + " *----------------------*");
    }

    public void onFinish(ITestContext testContext) {
        String str = "Test " + testContext.getCurrentXmlTest().getName() + " finish";
        log.info("*----------------------* " + str + " *----------------------*");
    }
}
