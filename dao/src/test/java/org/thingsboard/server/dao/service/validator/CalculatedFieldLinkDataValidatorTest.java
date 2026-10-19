// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.cf.CalculatedFieldLink;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.CalculatedFieldLinkId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.cf.CalculatedFieldLinkDao;
import org.thingsboard.server.dao.exception.DataValidationException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = CalculatedFieldLinkDataValidator.class)
public class CalculatedFieldLinkDataValidatorTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("2ba09d99-6143-43dc-b645-381fc0c43ebe"));
    private final CalculatedFieldLinkId CALCULATED_FIELD_LINK_ID = new CalculatedFieldLinkId(UUID.fromString("a5609ef4-cb42-43ce-9b23-e090a4878d1c"));

    @MockitoBean
    private CalculatedFieldLinkDao calculatedFieldLinkDao;
    @MockitoSpyBean
    private CalculatedFieldLinkDataValidator validator;

    @Test
    public void testUpdateNonExistingCalculatedField() {
        CalculatedFieldLink calculatedFieldLink = new CalculatedFieldLink(CALCULATED_FIELD_LINK_ID);
        calculatedFieldLink.setCalculatedFieldId(new CalculatedFieldId(UUID.fromString("136477af-fd07-4498-b9c9-54fe50e82992")));

        given(calculatedFieldLinkDao.findById(TENANT_ID, CALCULATED_FIELD_LINK_ID.getId())).willReturn(null);

        assertThatThrownBy(() -> validator.validateUpdate(TENANT_ID, calculatedFieldLink))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't update non existing calculated field link!");
    }

}
