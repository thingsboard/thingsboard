// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.DeviceSearchQueryFilter;
import org.thingsboard.server.common.data.util.CollectionsUtil;
import org.thingsboard.server.edqs.data.EntityData;
import org.thingsboard.server.edqs.data.ProfileAwareData;
import org.thingsboard.server.edqs.data.RelationInfo;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DeviceSearchQueryProcessor extends AbstractEntitySearchQueryProcessor<DeviceSearchQueryFilter> {

    private final Set<UUID> entityProfileIds = new HashSet<>();

    public DeviceSearchQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (DeviceSearchQueryFilter) query.getEntityFilter());
        if (CollectionsUtil.isNotEmpty(filter.getDeviceTypes())) {
            var profileNamesSet = new HashSet<>(this.filter.getDeviceTypes());
            for (EntityData<?> dp : repo.getEntitySet(EntityType.DEVICE_PROFILE)) {
                if (profileNamesSet.contains(dp.getFields().getName())) {
                    entityProfileIds.add(dp.getId());
                }
            }
        }
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.DEVICE;
    }

    @Override
    protected boolean check(RelationInfo relationInfo) {
        return super.check(relationInfo) &&
                (entityProfileIds.isEmpty() || entityProfileIds.contains(((ProfileAwareData<?>) relationInfo.getTarget()).getFields().getProfileId()));
    }

}
