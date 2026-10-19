// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
import org.thingsboard.server.dao.exception.DataValidationException;
import org.thingsboard.server.dao.usagerecord.DefaultApiLimitService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = CalculatedFieldDataValidator.class)
public class CalculatedFieldDataValidatorTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("7b5229e9-166e-41a9-a257-3b1dafad1b04"));
    private final CalculatedFieldId CALCULATED_FIELD_ID = new CalculatedFieldId(UUID.fromString("060fbe45-fbb2-4549-abf3-f72a6be3cb9f"));

    @MockitoBean
    private CalculatedFieldDao calculatedFieldDao;
    @MockitoBean
    private DefaultApiLimitService apiLimitService;
    @MockitoSpyBean
    private CalculatedFieldDataValidator validator;

    @Test
    public void testUpdateNonExistingCalculatedField() {
        CalculatedField calculatedField = new CalculatedField(CALCULATED_FIELD_ID);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test");

        given(calculatedFieldDao.findById(TENANT_ID, CALCULATED_FIELD_ID.getId())).willReturn(null);

        assertThatThrownBy(() -> validator.validateUpdate(TENANT_ID, calculatedField))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't update non existing calculated field!");
    }

}
