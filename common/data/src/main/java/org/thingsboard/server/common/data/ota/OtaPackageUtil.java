// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
