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
package org.thingsboard.server.dao.service.validator;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.asset.AssetDao;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.exception.DataValidationException;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.willReturn;

@SpringBootTest(classes = AssetDataValidator.class)
@Slf4j
class AssetDataValidatorTest {

    @MockBean
    AssetDao assetDao;
    @MockBean
    TenantService tenantService;
    @MockBean
    CustomerDao customerDao;
    @Autowired
    AssetDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "coffee", "1", "big box", "世界", "!", "--", "~!@#$%^&*()_+=-/|\\[]{};:'`\"?<>,.", "\uD83D\uDC0C", "\041",
            "Gdy Pomorze nie pomoże, to pomoże może morze, a gdy morze nie pomoże, to pomoże może Gdańsk",
    })
    void testAssetName_thenOK(final String name) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(name);
        validator.validateDataImpl(tenantId, asset);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", " ", "  ", "\n", "\r\n", "\t", "\000", "\000\000", "\001", "\002", "\040", "\u0000", "\u0000\u0000",
            "F0929906\000\000\000\000\000\000\000\000\000", "\000\000\000F0929906",
            "\u0000F0929906", "F092\u00009906", "F0929906\u0000"
    })
    void testAssetName_thenDataValidationException(final String name) {
        Asset asset = new Asset();
        asset.setTenantId(tenantId);
        asset.setName(name);

        DataValidationException exception = Assertions.assertThrows(DataValidationException.class, () -> validator.validateDataImpl(tenantId, asset));
        log.warn("Exception message: {}", exception.getMessage());
        assertThat(exception.getMessage()).as("message Asset name").containsPattern("Asset name .*");
    }

}
