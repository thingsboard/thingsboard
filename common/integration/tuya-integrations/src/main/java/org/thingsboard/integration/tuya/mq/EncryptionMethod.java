// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.tuya.mq;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EncryptionMethod {

    AES_ECB("AES", "aes_ecb", "AES/ECB/PKCS5Padding", 0, 0),
    AES_GCM("AES","aes_gcm", "AES/GCM/NoPadding", 128, 12);

    private final String algorithm;
    private final String code;
    private final String transform;
    private final int tagBits;
    private final int nonceLen;

    public static EncryptionMethod forCode(String code) {
        for (EncryptionMethod em : values()) {
            if (em.code.equalsIgnoreCase(code)) {
                return em;
            }
        }

        return null;
    }

}
