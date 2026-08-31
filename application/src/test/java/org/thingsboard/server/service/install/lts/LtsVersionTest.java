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
package org.thingsboard.server.service.install.lts;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LtsVersionTest {

    @ParameterizedTest
    @CsvSource({
            "4.2.1, 4, 2, 1, 0",
            "4.2.0, 4, 2, 0, 0",
            "4.2, 4, 2, 0, 0",
            "4.0.1.2, 4, 0, 1, 2",
            "4, 4, 0, 0, 0",
            "10.20.30.40, 10, 20, 30, 40"
    })
    void parsesValidVersions(String input, int major, int minor, int maintenance, int patch) {
        LtsVersion v = LtsVersion.parse(input);
        assertEquals(major, v.major());
        assertEquals(minor, v.minor());
        assertEquals(maintenance, v.maintenance());
        assertEquals(patch, v.patch());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "a.b.c", "1.2.y.x", "1.x.3", ""})
    void throwsOnInvalidVersions(String input) {
        assertThrows(IllegalArgumentException.class, () -> LtsVersion.parse(input));
    }

    @Test
    void throwsOnNull() {
        assertThrows(IllegalArgumentException.class, () -> LtsVersion.parse(null));
    }

    @Test
    void comparesByEachComponent() {
        assertTrue(LtsVersion.parse("4.2.2.3").compareTo(LtsVersion.parse("4.2.2.2")) > 0);
        assertTrue(LtsVersion.parse("4.2.2.0").compareTo(LtsVersion.parse("4.2.1.9")) > 0);
        assertTrue(LtsVersion.parse("4.3.0.0").compareTo(LtsVersion.parse("4.2.9.9")) > 0);
        assertEquals(0, LtsVersion.parse("4.2.2.3").compareTo(LtsVersion.parse("4.2.2.3")));
    }

    @Test
    void sameFamilyComparesMajorAndMinorOnly() {
        assertTrue(LtsVersion.parse("4.2.2.3").sameFamily(LtsVersion.parse("4.2.0.0")));
        assertFalse(LtsVersion.parse("4.3.0.0").sameFamily(LtsVersion.parse("4.2.9.9")));
        assertFalse(LtsVersion.parse("5.2.0.0").sameFamily(LtsVersion.parse("4.2.0.0")));
    }

    @Test
    void toStringIsFourComponentDotForm() {
        assertEquals("4.2.2.3", LtsVersion.parse("4.2.2.3").toString());
        assertEquals("4.2.0.0", LtsVersion.parse("4.2").toString());
    }
}
