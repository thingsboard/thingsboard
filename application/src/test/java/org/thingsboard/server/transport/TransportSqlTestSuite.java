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
package org.thingsboard.server.transport;

import org.junit.extensions.cpsuite.ClasspathSuite;
import org.junit.runner.RunWith;

@RunWith(ClasspathSuite.class)
@ClasspathSuite.ClassnameFilters({
//        "org.thingsboard.server.transport.*.rpc.*Test",
//        "org.thingsboard.server.transport.*.telemetry.timeseries.sql.*Test",
//        "org.thingsboard.server.transport.*.telemetry.attributes.*Test",
//        "org.thingsboard.server.transport.*.attributes.updates.*Test",
//        "org.thingsboard.server.transport.*.attributes.request.*Test",
        "org.thingsboard.server.transport.mqtt.claim.*Test",
//        "org.thingsboard.server.transport.*.provision.*Test",
//        "org.thingsboard.server.transport.*.credentials.*Test",
//        "org.thingsboard.server.transport.lwm2m.*.sql.*Test"
})
public class TransportSqlTestSuite {

}
