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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
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

    @MockBean
    private CalculatedFieldLinkDao calculatedFieldLinkDao;
    @SpyBean
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
