// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.exception.DataValidationException;

class ConstraintValidatorTest {

    @Test
    void validateFields() {
        StringDataEntry stringDataEntryValid = new StringDataEntry("key", "value");
        StringDataEntry stringDataEntryInvalid1 = new StringDataEntry("<object type=\"text/html\"><script>alert(document)</script></object>", "value");

        Assert.assertThrows(DataValidationException.class, () -> ConstraintValidator.validateFields(stringDataEntryInvalid1));
        ConstraintValidator.validateFields(stringDataEntryValid);
    }

}
