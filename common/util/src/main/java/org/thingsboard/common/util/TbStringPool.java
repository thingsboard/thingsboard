// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.common.util;

import lombok.Getter;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.concurrent.ConcurrentMap;

public class TbStringPool {

    @Getter
    private static final ConcurrentMap<String, String> pool = new ConcurrentReferenceHashMap<>();

    public static String intern(String data) {
        if (data == null) {
            return null;
        }
        return pool.computeIfAbsent(data, str -> str);
    }

}
