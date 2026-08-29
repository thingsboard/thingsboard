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
package org.thingsboard.server.common.data;

import com.datastax.oss.driver.api.core.uuid.Uuids;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.UUID;

/**
 * Created by ashvayka on 14.07.17.
 */
public class UUIDConverterTest {

    @Test
    public void basicUuidToStringTest() {
        UUID original = UUID.fromString("58e0a7d7-eebc-11d8-9669-0800200c9a66");
        String result = UUIDConverter.fromTimeUUID(original);
        Assertions.assertEquals("1d8eebc58e0a7d796690800200c9a66", result);
    }


    @Test
    public void basicUuid() {
        UUID result = UUIDConverter.fromString("1e746126eaaefa6a91992ebcb67fe33");
        Assertions.assertEquals(
                UUID.fromString("6eaaefa6-4612-11e7-a919-92ebcb67fe33"),
                result
        );
    }

    @Test
    public void basicUuidConversion() {
        UUID original = UUID.fromString("3dd11790-abf2-11ea-b151-83a091b9d4cc");
        Assertions.assertEquals(Uuids.unixTimestamp(original), 1591886749577L);
    }

    @Test
    public void basicStringToUUIDTest() {
        UUID result = UUIDConverter.fromString("1d8eebc58e0a7d796690800200c9a66");
        Assertions.assertEquals(UUID.fromString("58e0a7d7-eebc-11d8-9669-0800200c9a66"), result);
    }

    @Test
    public void nonV1UuidToStringTest() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            UUIDConverter.fromTimeUUID(UUID.fromString("58e0a7d7-eebc-01d8-9669-0800200c9a66"));
        });
    }

    @Test
    public void basicUuidComparisonTest() {
        for (int i = 0; i < 100000; i++) {
            long ts = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 365 * 10;
            long before = (long) (Math.random() * ts);
            long after = (long) (Math.random() * ts);
            if (before > after) {
                long tmp = after;
                after = before;
                before = tmp;
            }

            String beforeStr = UUIDConverter.fromTimeUUID(Uuids.startOf(before));
            String afterStr = UUIDConverter.fromTimeUUID(Uuids.startOf(after));

            Assertions.assertTrue(
                    afterStr.compareTo(beforeStr) >= 0,
                    "Expected chronological UUID string ordering. Before: "
                            + before + " | " + beforeStr
                            + ", After: " + after + " | " + afterStr
            );
        }
    }


}
