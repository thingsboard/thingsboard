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
package org.thingsboard.server.transport.snmp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class SnmpTestV2 {
    public static void main(String[] args) throws IOException {
        SnmpDeviceSimulatorV2 device = new SnmpDeviceSimulatorV2(1610, "public");

        device.start();
        Map<String, String> mappings = new HashMap<>();
        for (int i = 1; i <= 500; i++) {
            String oid = String.format(".1.3.6.1.2.1.%s.1.52", i);
            mappings.put(oid, "value_" + i);
        }
        device.setUpMappings(mappings);

        new Scanner(System.in).nextLine();
    }

}
