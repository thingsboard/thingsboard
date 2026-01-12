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
package org.thingsboard.server.transport.snmp;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.mp.MPv3;
import org.snmp4j.smi.OctetString;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

public class SnmpTestV3 {
    public static void main(String[] args) throws IOException {
        SnmpDeviceSimulatorV3 device = new SnmpDeviceSimulatorV3(new CommandProcessor(new OctetString(MPv3.createLocalEngineID())) {
            @Override
            public void processPdu(CommandResponderEvent event) {
                System.out.println("event: " + event);
            }
        });
        device.start("0.0.0.0", "1610");

        device.setUpMappings(Map.of(
                ".1.3.6.1.2.1.1.1.50", "12",
                ".1.3.6.1.2.1.2.1.52", "56",
                ".1.3.6.1.2.1.3.1.54", "yes",
                ".1.3.6.1.2.1.7.1.58", ""
        ));

        new Scanner(System.in).nextLine();
    }
}
