// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.extension;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.AbstractJpaDaoTest;
import org.thingsboard.server.dao.extension.ExtensionDao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression coverage for R4-3: the tenant list query is JPQL, so sorting must use entity attribute
 * names. Sorting by createdTime used to inject the SQL column name (created_time) and fail with a
 * Hibernate SemanticException -> HTTP 500.
 */
public class JpaExtensionDaoTest extends AbstractJpaDaoTest {

    @Autowired
    private ExtensionDao extensionDao;

    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private final List<Extension> saved = new ArrayList<>();

    @Before
    public void setUp() {
        long baseTime = 1_700_000_000_000L;
        for (int i = 0; i < 3; i++) {
            Extension extension = new Extension();
            extension.setTenantId(tenantId);
            extension.setName("ext-" + (char) ('c' - i)); // names c, b, a — reverse of creation order
            extension.setSlug("test-sort-ext-" + i);
            extension.setState(ExtensionState.PROVISIONING);
            extension.setCreatedTime(baseTime + i * 1000L);
            saved.add(extensionDao.save(tenantId, extension));
        }
    }

    @After
    public void tearDown() {
        saved.forEach(extension -> extensionDao.deleteByTenantIdAndId(tenantId, extension.getId()));
    }

    @Test
    public void sortByCreatedTimeDoesNotFailAndOrdersCorrectly() {
        PageData<Extension> desc = extensionDao.findByTenantId(tenantId.getId(),
                new PageLink(10, 0, null, new SortOrder("createdTime", SortOrder.Direction.DESC)));

        assertThat(desc.getData()).hasSize(3);
        assertThat(desc.getData().get(0).getCreatedTime()).isGreaterThan(desc.getData().get(2).getCreatedTime());

        PageData<Extension> asc = extensionDao.findByTenantId(tenantId.getId(),
                new PageLink(10, 0, null, new SortOrder("createdTime", SortOrder.Direction.ASC)));

        assertThat(asc.getData()).hasSize(3);
        assertThat(asc.getData().get(0).getId()).isEqualTo(desc.getData().get(2).getId());
    }

    @Test
    public void sortByNameAndStateStillWork() {
        PageData<Extension> byName = extensionDao.findByTenantId(tenantId.getId(),
                new PageLink(10, 0, null, new SortOrder("name", SortOrder.Direction.ASC)));

        assertThat(byName.getData()).hasSize(3);
        assertThat(byName.getData().get(0).getName()).isEqualTo("ext-a");
        assertThat(byName.getData().get(2).getName()).isEqualTo("ext-c");

        PageData<Extension> byState = extensionDao.findByTenantId(tenantId.getId(),
                new PageLink(10, 0, null, new SortOrder("state", SortOrder.Direction.ASC)));
        assertThat(byState.getData()).hasSize(3);
    }

    @Test
    public void infosQueryAlsoSortsByCreatedTime() {
        // findInfosByTenantId shares the JPQL query; make sure the Info path sorts too
        assertThat(extensionDao.findInfosByTenantId(tenantId.getId(),
                new PageLink(10, 0, null, new SortOrder("createdTime", SortOrder.Direction.DESC)))
                .getData()).hasSize(3);
    }

}
