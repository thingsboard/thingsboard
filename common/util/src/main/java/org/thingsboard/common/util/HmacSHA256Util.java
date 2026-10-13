// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

public final class HmacSHA256Util {

    public static byte[] sign(String data, byte[] keyBytes) throws InvalidKeyException, NoSuchAlgorithmException {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        return sign(dataBytes, keyBytes);
    }

    public static byte[] sign(byte[] dataBytes, byte[] keyBytes) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec signingKey = new SecretKeySpec(keyBytes, "HmacSHA256");
        mac.init(signingKey);
        return mac.doFinal(dataBytes);
    }


}
