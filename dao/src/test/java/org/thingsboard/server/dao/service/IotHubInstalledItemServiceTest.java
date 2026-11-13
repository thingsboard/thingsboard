// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.service;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.server.common.data.iot_hub.IotHubInstalledItem;
import org.thingsboard.server.common.data.iot_hub.WidgetInstalledItemDescriptor;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.dao.iot_hub.IotHubInstalledItemService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class IotHubInstalledItemServiceTest extends AbstractServiceTest {

    @Autowired
    IotHubInstalledItemService installedItemService;

    private final UUID trackerItemId = UUID.randomUUID();
    private final UUID sensorItemId = UUID.randomUUID();
    private final UUID unpublishedItemId = UUID.randomUUID();
    private final UUID trackerPublished = UUID.randomUUID();
    private final UUID sensorPublished = UUID.randomUUID();

    private void install(String name, UUID itemId, UUID versionId) {
        IotHubInstalledItem item = new IotHubInstalledItem();
        item.setTenantId(tenantId);
        item.setItemId(itemId);
        item.setItemVersionId(versionId);
        item.setItemName(name);
        item.setItemType("WIDGET");
        item.setVersion("1.0.0");
        item.setDescriptor(new WidgetInstalledItemDescriptor());
        installedItemService.save(tenantId, item);
    }

    /** a and c are up to date, b and d have an update (d is a second install of a's item in an older version), e has no published version. */
    private void installFive() {
        install("a", sensorItemId, sensorPublished);
        install("b", trackerItemId, UUID.randomUUID());
        install("c", sensorItemId, sensorPublished);
        install("d", sensorItemId, UUID.randomUUID());
        install("e", unpublishedItemId, UUID.randomUUID());
    }

    private Map<UUID, UUID> published() {
        return Map.of(trackerItemId, trackerPublished, sensorItemId, sensorPublished);
    }

    private static List<String> names(PageData<IotHubInstalledItem> page) {
        return page.getData().stream().map(IotHubInstalledItem::getItemName).toList();
    }

    @Test
    public void updatesFirst_itemsWithAnUpdateComeFirst_eachGroupInThePageSort() {
        installFive();
        PageLink byName = new PageLink(10, 0, null, new SortOrder("itemName", SortOrder.Direction.ASC));

        PageData<IotHubInstalledItem> page = installedItemService.findByTenantIdUpdatesFirst(tenantId, null, null, published(), byName);

        assertThat(names(page)).containsExactly("b", "d", "a", "c", "e");
        assertThat(page.getTotalElements()).isEqualTo(5);
    }

    @Test
    public void updatesFirst_pagesKeepTheOrderAndTheTotal() {
        installFive();
        SortOrder byNameDesc = new SortOrder("itemName", SortOrder.Direction.DESC);

        PageData<IotHubInstalledItem> first = installedItemService.findByTenantIdUpdatesFirst(tenantId, null, null, published(),
                new PageLink(2, 0, null, byNameDesc));
        PageData<IotHubInstalledItem> second = installedItemService.findByTenantIdUpdatesFirst(tenantId, null, null, published(),
                new PageLink(2, 1, null, byNameDesc));

        assertThat(names(first)).containsExactly("d", "b");
        assertThat(names(second)).containsExactly("e", "c");
        assertThat(first.getTotalElements()).isEqualTo(5);
        assertThat(first.hasNext()).isTrue();
    }

    @Test
    public void updatesFirst_keepsTheFilters() {
        installFive();
        // "b" is in no item type or version, only in the name of b
        PageLink search = new PageLink(10, 0, "b", new SortOrder("itemName", SortOrder.Direction.ASC));

        PageData<IotHubInstalledItem> page = installedItemService.findByTenantIdUpdatesFirst(tenantId, List.of("WIDGET"), null, published(), search);

        assertThat(names(page)).containsExactly("b");
        assertThat(page.getTotalElements()).isEqualTo(1);
    }

    @Test
    public void updatesFirst_withoutPublishedVersions_isThePageSortOnly() {
        installFive();
        PageLink byName = new PageLink(10, 0, null, new SortOrder("itemName", SortOrder.Direction.ASC));

        PageData<IotHubInstalledItem> page = installedItemService.findByTenantIdUpdatesFirst(tenantId, null, null, Map.of(), byName);

        assertThat(names(page)).containsExactly("a", "b", "c", "d", "e");
    }

}
