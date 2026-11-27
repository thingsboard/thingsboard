// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The /api/ext-auth check asks who owns a slug on every data-plane request. The service must answer
 * from the tenant-only lookup and must not go to the database for a blank slug.
 */
class DefaultExtensionServiceSlugOwnerTest {

    private ExtensionDao extensionDao;
    private DefaultExtensionService service;

    @BeforeEach
    void setUp() {
        extensionDao = mock(ExtensionDao.class);
        service = new DefaultExtensionService(extensionDao, null, null, mock(ApiLimitService.class), null);
    }

    @Test
    void returnsTheOwnerOfTheSlug() {
        TenantId owner = TenantId.fromUUID(UUID.randomUUID());
        when(extensionDao.findTenantIdBySlug("acme-9f3a")).thenReturn(owner);

        assertThat(service.findTenantIdBySlug("acme-9f3a")).isEqualTo(owner);
    }

    @Test
    void returnsNullForAnUnknownSlug() {
        assertThat(service.findTenantIdBySlug("ghost-0000")).isNull();
    }

    @Test
    void doesNotQueryForABlankSlug() {
        assertThat(service.findTenantIdBySlug(null)).isNull();
        assertThat(service.findTenantIdBySlug("  ")).isNull();

        verifyNoInteractions(extensionDao);
    }

}
