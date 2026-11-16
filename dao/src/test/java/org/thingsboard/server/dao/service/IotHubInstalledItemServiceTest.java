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

import java.util.HashMap;
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
    public void withUpdates_onlyTheItemsWithAnUpdate_inThePageSort() {
        installFive();
        PageLink byName = new PageLink(10, 0, null, new SortOrder("itemName", SortOrder.Direction.DESC));

        PageData<IotHubInstalledItem> page = installedItemService.findByTenantIdWithUpdates(tenantId, null, published(), byName);

        assertThat(names(page)).containsExactly("d", "b");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    public void withUpdates_pagesAndTotal() {
        installFive();
        SortOrder byName = new SortOrder("itemName", SortOrder.Direction.ASC);

        PageData<IotHubInstalledItem> first = installedItemService.findByTenantIdWithUpdates(tenantId, null, published(),
                new PageLink(1, 0, null, byName));
        PageData<IotHubInstalledItem> second = installedItemService.findByTenantIdWithUpdates(tenantId, null, published(),
                new PageLink(1, 1, null, byName));

        assertThat(names(first)).containsExactly("b");
        assertThat(names(second)).containsExactly("d");
        assertThat(first.getTotalElements()).isEqualTo(2);
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isFalse();
    }

    @Test
    public void withUpdates_keepsTheSearchAndTheTypes() {
        installFive();
        // "b" is in no item type or version, only in the name of b
        PageLink search = new PageLink(10, 0, "b", new SortOrder("itemName", SortOrder.Direction.ASC));

        assertThat(names(installedItemService.findByTenantIdWithUpdates(tenantId, List.of("WIDGET"), published(), search)))
                .containsExactly("b");
        assertThat(installedItemService.findByTenantIdWithUpdates(tenantId, List.of("DEVICE"), published(),
                new PageLink(10)).getData()).isEmpty();
    }

    @Test
    public void plainList_sameQuery_isNotFiltered() {
        installFive();
        PageLink byName = new PageLink(10, 0, null, new SortOrder("itemName", SortOrder.Direction.ASC));

        assertThat(names(installedItemService.findByTenantId(tenantId, null, null, byName))).containsExactly("a", "b", "c", "d", "e");
        assertThat(names(installedItemService.findByTenantId(tenantId, null, sensorItemId, byName))).containsExactly("a", "c", "d");
    }

    @Test
    public void withUpdates_ignoresItemsTheTenantHasNotInstalled() {
        installFive();
        Map<UUID, UUID> published = new HashMap<>(published());
        for (int i = 0; i < 20_000; i++) {
            published.put(UUID.randomUUID(), UUID.randomUUID());
        }

        PageData<IotHubInstalledItem> page = installedItemService.findByTenantIdWithUpdates(tenantId, null, published,
                new PageLink(10, 0, null, new SortOrder("itemName", SortOrder.Direction.ASC)));

        // 40k bind parameters would be over PostgreSQL's limit of 32767
        assertThat(names(page)).containsExactly("b", "d");
    }

    @Test
    public void withUpdates_withoutPublishedVersions_isEmpty() {
        installFive();
        Map<UUID, UUID> unpublished = new HashMap<>();
        unpublished.put(unpublishedItemId, null);

        assertThat(installedItemService.findByTenantIdWithUpdates(tenantId, null, Map.of(), new PageLink(10)).getData()).isEmpty();
        assertThat(installedItemService.findByTenantIdWithUpdates(tenantId, null, unpublished, new PageLink(10)).getData()).isEmpty();
    }

}
