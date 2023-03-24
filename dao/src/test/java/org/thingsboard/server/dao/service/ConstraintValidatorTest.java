package org.thingsboard.server.dao.service;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.kv.JsonDataEntry;
import org.thingsboard.server.common.data.kv.StringDataEntry;
import org.thingsboard.server.dao.exception.DataValidationException;

class ConstraintValidatorTest {

    @Test
    void validateFields() {
        StringDataEntry stringDataEntryValid = new StringDataEntry("key", "value");

        StringDataEntry stringDataEntryInvalid1 = new StringDataEntry("<object type=\"text/html\"><script>alert(document)</script></object>", "value");
        StringDataEntry stringDataEntryInvalid2 = new StringDataEntry("key", "<object type=\"text/html\"><script>alert(document)</script></object>");

        JsonDataEntry jsonDataEntryInvalid = new JsonDataEntry("key", "{\"value\": <object type=\"text/html\"><script>alert(document)</script></object>}");

        Assert.assertThrows(DataValidationException.class, () -> ConstraintValidator.validateFields(stringDataEntryInvalid1));
        Assert.assertThrows(DataValidationException.class, () -> ConstraintValidator.validateFields(stringDataEntryInvalid2));
        Assert.assertThrows(DataValidationException.class, () -> ConstraintValidator.validateFields(jsonDataEntryInvalid));
        ConstraintValidator.validateFields(stringDataEntryValid);
    }
}