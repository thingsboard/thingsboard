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
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.exception.DataValidationException;

class ConstraintValidatorTest {

    private static final int MIN_IN_MS = 60000;
    private static final int _1M = 1_000_000;

    @Test
    void validateFields() {
        StringDataEntry stringDataEntryValid = new StringDataEntry("key", "value");
        StringDataEntry stringDataEntryInvalid1 = new StringDataEntry("<object type=\"text/html\"><script>alert(document)</script></object>", "value");

        Assert.assertThrows(DataValidationException.class, () -> ConstraintValidator.validateFields(stringDataEntryInvalid1));
        ConstraintValidator.validateFields(stringDataEntryValid);
    }

    @Test
    void validatePerMinute() {
        StringDataEntry stringDataEntryValid = new StringDataEntry("key", "value");

        long start = System.currentTimeMillis();
        for (int i = 0; i < _1M; i++) {
            ConstraintValidator.validateFields(stringDataEntryValid);
        }
        long end = System.currentTimeMillis();

        Assertions.assertTrue(MIN_IN_MS > end - start);
    }
}
