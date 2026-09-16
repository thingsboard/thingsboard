// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
