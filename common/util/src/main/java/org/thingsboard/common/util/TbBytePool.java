// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import com.google.common.hash.Hashing;
import lombok.Getter;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.concurrent.ConcurrentMap;

public class TbBytePool {

    @Getter
    private static final ConcurrentMap<String, byte[]> pool = new ConcurrentReferenceHashMap<>();

    public static byte[] intern(byte[] data) {
        if (data == null) {
            return null;
        }
        var checksum = Hashing.sha512().hashBytes(data).toString();
        return pool.computeIfAbsent(checksum, c -> data);
    }

}
