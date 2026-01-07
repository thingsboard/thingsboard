/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;
import org.testng.internal.ConstructorOrMethod;
import org.thingsboard.server.msa.DisableUIListeners;

public class RetryAnalyzer implements IRetryAnalyzer {

    private int retryCount = 0;
    private static final int MAX_RETRY_COUNT = 2;

    @Override
    public boolean retry(ITestResult result) {
        ConstructorOrMethod consOrMethod = result.getMethod().getConstructorOrMethod();
        DisableUIListeners disable = consOrMethod.getMethod().getDeclaringClass().getAnnotation(DisableUIListeners.class);
        if (disable != null) {
            return false;
        }
        if (retryCount < MAX_RETRY_COUNT) {
            System.out.printf("Retrying test %s for the %d time(s).%n", result.getName(), retryCount + 1);
            retryCount++;
            return true;
        }
        return false;
    }
}
