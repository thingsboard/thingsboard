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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.thingsboard.common.util.JacksonUtil;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Collectors;

public class SnmpTestV2 {

    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) throws IOException {
        Map<String, String> mappings = new LinkedHashMap<>();
        for (int i = 1; i <= 50; i++) {
            String oid = String.format("1.3.6.1.2.1.%s.1.52", i);
            mappings.put(oid, "value_" + i);
        }

        SnmpDeviceSimulatorV2 device = new SnmpDeviceSimulatorV2(1610, "public", mappings);
        device.start();

        System.out.println("Hosting the following values:\n" + mappings.entrySet().stream()
                .map(entry -> entry.getKey() + " - " + entry.getValue())
                .collect(Collectors.joining("\n")));

        scanner.nextLine();
    }

    private static void inputTraps(SnmpDeviceSimulatorV2 client) throws IOException {
        while (true) {
            String data = scanner.nextLine();
            if (!data.isEmpty()) {
                client.sendTrap("127.0.0.1", 1620, Map.of(
                        "1.3.6.1.2.1.266.1.52", data + " (266)",
                        "1.3.6.1.2.1.267.1.52", data + " (267)"
                ));
            }
        }
    }

    private static void updateDeviceProfile(String file) throws Exception {
        File profileFile = new File(file);
        JsonNode deviceProfile = JacksonUtil.OBJECT_MAPPER.readTree(profileFile);
        ArrayNode mappingsJson = (ArrayNode) deviceProfile.at("/profileData/transportConfiguration/communicationConfigs/0/mappings");
        for (int i = 1; i <= 50; i++) {
            String oid = String.format(".1.3.6.1.2.1.%s.1.52", i);
            mappingsJson.add(JacksonUtil.newObjectNode()
                    .put("oid", oid)
                    .put("key", "key_" + i)
                    .put("dataType", "STRING"));
        }
        JacksonUtil.OBJECT_MAPPER.writeValue(profileFile, deviceProfile);
    }

}
