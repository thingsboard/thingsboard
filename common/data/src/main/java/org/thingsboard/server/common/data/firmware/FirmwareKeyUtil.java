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
package org.thingsboard.server.common.data.firmware;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.thingsboard.server.common.data.firmware.FirmwareType.FIRMWARE;
import static org.thingsboard.server.common.data.firmware.FirmwareType.SOFTWARE;

public class FirmwareKeyUtil {

    public static final List<String> ALL_FW_ATTRIBUTE_KEYS;

    public static final List<String> ALL_SW_ATTRIBUTE_KEYS;

    static {
        ALL_FW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (FirmwareKey key : FirmwareKey.values()) {
            ALL_FW_ATTRIBUTE_KEYS.add(getAttributeKey(FIRMWARE, key));

        }

        ALL_SW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (FirmwareKey key : FirmwareKey.values()) {
            ALL_SW_ATTRIBUTE_KEYS.add(getAttributeKey(SOFTWARE, key));

        }
    }

    public static List<String> getAttributeKeys(FirmwareType firmwareType) {
        switch (firmwareType) {
            case FIRMWARE:
                return ALL_FW_ATTRIBUTE_KEYS;
            case SOFTWARE:
                return ALL_SW_ATTRIBUTE_KEYS;
        }
        return Collections.emptyList();
    }

    public static String getAttributeKey(FirmwareType type, FirmwareKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    public static String getTargetTelemetryKey(FirmwareType type, FirmwareKey key) {
        return getTelemetryKey("target_", type, key);
    }

    public static String getCurrentTelemetryKey(FirmwareType type, FirmwareKey key) {
        return getTelemetryKey("current_", type, key);
    }

    private static String getTelemetryKey(String prefix, FirmwareType type, FirmwareKey key) {
        return prefix + type.getKeyPrefix() + "_" + key.getValue();
    }

    public static String getTelemetryKey(FirmwareType type, FirmwareKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }
}
