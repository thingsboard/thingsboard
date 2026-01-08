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
package org.thingsboard.server.common.data.ota;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.server.common.data.HasOtaPackage;
import org.thingsboard.server.common.data.id.OtaPackageId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
public class OtaPackageUtil {

    public static final List<String> ALL_FW_ATTRIBUTE_KEYS;

    public static final List<String> ALL_SW_ATTRIBUTE_KEYS;

    static {
        ALL_FW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (OtaPackageKey key : OtaPackageKey.values()) {
            ALL_FW_ATTRIBUTE_KEYS.add(getAttributeKey(OtaPackageType.FIRMWARE, key));

        }

        ALL_SW_ATTRIBUTE_KEYS = new ArrayList<>();
        for (OtaPackageKey key : OtaPackageKey.values()) {
            ALL_SW_ATTRIBUTE_KEYS.add(getAttributeKey(OtaPackageType.SOFTWARE, key));

        }
    }

    public static List<String> getAttributeKeys(OtaPackageType firmwareType) {
        switch (firmwareType) {
            case FIRMWARE:
                return ALL_FW_ATTRIBUTE_KEYS;
            case SOFTWARE:
                return ALL_SW_ATTRIBUTE_KEYS;
        }
        return Collections.emptyList();
    }

    public static String getAttributeKey(OtaPackageType type, OtaPackageKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    public static String getTargetTelemetryKey(OtaPackageType type, OtaPackageKey key) {
        return getTelemetryKey("target_", type, key);
    }

    public static String getCurrentTelemetryKey(OtaPackageType type, OtaPackageKey key) {
        return getTelemetryKey("current_", type, key);
    }

    private static String getTelemetryKey(String prefix, OtaPackageType type, OtaPackageKey key) {
        return prefix + type.getKeyPrefix() + "_" + key.getValue();
    }

    public static String getTelemetryKey(OtaPackageType type, OtaPackageKey key) {
        return type.getKeyPrefix() + "_" + key.getValue();
    }

    public static OtaPackageId getOtaPackageId(HasOtaPackage entity, OtaPackageType type) {
        switch (type) {
            case FIRMWARE:
                return entity.getFirmwareId();
            case SOFTWARE:
                return entity.getSoftwareId();
            default:
                log.warn("Unsupported ota package type: [{}]", type);
                return null;
        }
    }

    public static <T> T getByOtaPackageType(Supplier<T> firmwareSupplier, Supplier<T> softwareSupplier, OtaPackageType type) {
        switch (type) {
            case FIRMWARE:
                return firmwareSupplier.get();
            case SOFTWARE:
                return softwareSupplier.get();
            default:
                throw new RuntimeException("Unsupported OtaPackage type: " + type);
        }
    }
}
