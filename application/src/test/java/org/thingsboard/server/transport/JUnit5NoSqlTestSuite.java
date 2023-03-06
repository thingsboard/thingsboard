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
package org.thingsboard.server.transport;

import lombok.extern.slf4j.Slf4j;
import org.junit.platform.suite.api.ExcludeClassNamePatterns;
import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;

@Suite
@SuiteDisplayName("JUnit5 test suite")
@SelectPackages("org.thingsboard.server.transport.mqtt.mqttv3.telemetry.timeseries.nosql")
@ExcludeClassNamePatterns({"^.*Abstract.*$"})
@Slf4j
public class JUnit5NoSqlTestSuite {

    static {
        log.warn("JUnit5NoSqlTestSuite constructor static"); //no static instance created
    }

    public JUnit5NoSqlTestSuite() {
        log.warn("JUnit5NoSqlTestSuite constructor");  //no instance created
    }

}