// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.cf;

import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldFilter;
import org.thingsboard.server.common.data.cf.CalculatedFieldInfo;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.EntityDaoService;

import java.util.List;
import java.util.function.Consumer;

public interface CalculatedFieldService extends EntityDaoService {

    CalculatedField save(CalculatedField calculatedField);

    CalculatedField save(CalculatedField calculatedField, boolean doValidate);

    CalculatedField findById(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    CalculatedField findByEntityIdAndTypeAndName(EntityId entityId, CalculatedFieldType type, String name);

    List<CalculatedFieldId> findCalculatedFieldIdsByEntityId(TenantId tenantId, EntityId entityId);

    List<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    /**
     * @deprecated reads one page per call with OFFSET and a full count, so iterating all pages costs O(N²/pageSize) rows;
     * use {@link #findAllCalculatedFields(int, Consumer)} instead. Scheduled for removal in the next major release.
     */
    @Deprecated(since = "4.4", forRemoval = true)
    PageData<CalculatedField> findAllCalculatedFields(PageLink pageLink);

    /**
     * @deprecated reads one page per call with OFFSET and a full count, so iterating all pages costs O(N²/pageSize) rows;
     * use {@link #findCalculatedFieldsByTenantId(TenantId, int, Consumer)} instead. Scheduled for removal in the next major release.
     */
    @Deprecated(since = "4.4", forRemoval = true)
    PageData<CalculatedField> findCalculatedFieldsByTenantId(TenantId tenantId, PageLink pageLink);

    /**
     * Passes all calculated fields to {@code pageConsumer} page by page: keyset pages of up to {@code pageSize} rows by id,
     * with no OFFSET and no count. A page is handed over after its query has finished, so no database connection is held
     * while the consumer runs. Fields with an unreadable configuration are left out.
     */
    void findAllCalculatedFields(int pageSize, Consumer<List<CalculatedField>> pageConsumer);

    /**
     * Same as {@link #findAllCalculatedFields(int, Consumer)}, limited to one tenant.
     */
    void findCalculatedFieldsByTenantId(TenantId tenantId, int pageSize, Consumer<List<CalculatedField>> pageConsumer);

    PageData<CalculatedFieldInfo> findCalculatedFieldsByTenantIdAndFilter(TenantId tenantId, CalculatedFieldFilter filter, PageLink pageLink);

    PageData<String> findCalculatedFieldNamesByTenantIdAndType(TenantId tenantId, CalculatedFieldType type, PageLink pageLink);

    PageData<CalculatedField> findCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId, CalculatedFieldType type, PageLink pageLink);

    void deleteCalculatedField(TenantId tenantId, CalculatedFieldId calculatedFieldId);

    int deleteAllCalculatedFieldsByEntityId(TenantId tenantId, EntityId entityId);

    boolean referencedInAnyCalculatedField(TenantId tenantId, EntityId referencedEntityId);

}
