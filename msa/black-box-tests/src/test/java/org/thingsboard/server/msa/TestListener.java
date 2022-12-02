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

import lombok.extern.slf4j.Slf4j;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

import static org.testng.internal.Utils.log;

@Slf4j
public class TestListener  extends TestListenerAdapter {

    @Override
    public void onTestStart(ITestResult result) {
        super.onTestStart(result);
        log.info("===>>> Test started: " + result.getName());
    }

    /**
     * Invoked when a test succeeds
     */
    @Override
    public void onTestSuccess(ITestResult result) {
        super.onTestSuccess(result);
        if (result != null) {
            log.info("<<<=== Test completed successfully: " + result.getName());
        }
    }

    /**
     * Invoked when a test fails
     */
    @Override
    public void onTestFailure(ITestResult result) {
        super.onTestFailure(result);
        log.info("<<<=== Test failed: " + result.getName());
    }
}
