/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
import java.util.Map;
import java.util.Scanner;

public class SnmpTestV2 {
    public static void main(String[] args) throws IOException {
        SnmpDeviceSimulatorV2 device = new SnmpDeviceSimulatorV2(1610, "public");

        device.start();
        device.setUpMappings(Map.of(
                ".1.3.6.1.2.1.1.1.50", "12",
                ".1.3.6.1.2.1.2.1.52", "56",
                ".1.3.6.1.2.1.3.1.54", "yes",
                ".1.3.6.1.2.1.7.1.58", ""
        ));


//        while (true) {
//            new Scanner(System.in).nextLine();
//            device.sendTrap("127.0.0.1", 1062, Map.of(".1.3.6.1.2.87.1.56", "12"));
//            System.out.println("sent");
//        }

//        Snmp snmp = new Snmp(device.transportMappings[0]);
//        device.snmp.addCommandResponder(event -> {
//            System.out.println(event);
//        });

        new Scanner(System.in).nextLine();
    }

}
