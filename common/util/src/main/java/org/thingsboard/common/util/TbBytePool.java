/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.common.util;

import com.google.common.hash.Hashing;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.concurrent.ConcurrentMap;

public class TbBytePool {

    private static final ConcurrentMap<String, byte[]> pool = new ConcurrentReferenceHashMap<>();

    public static byte[] intern(byte[] data) {
        if (data == null) {
            return null;
        }
        var checksum = Hashing.sha512().hashBytes(data).toString();
        return pool.computeIfAbsent(checksum, c -> data);
    }

    public static int size(){
        return pool.size();
    }

}
