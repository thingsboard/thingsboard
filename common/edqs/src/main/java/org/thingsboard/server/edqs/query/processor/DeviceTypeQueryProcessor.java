// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.query.processor;

import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.permission.QueryContext;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.edqs.query.EdqsQuery;
import org.thingsboard.server.edqs.repo.TenantRepo;

import java.util.List;

public class DeviceTypeQueryProcessor extends AbstractEntityProfileQueryProcessor<DeviceTypeFilter> {

    public DeviceTypeQueryProcessor(TenantRepo repo, QueryContext ctx, EdqsQuery query) {
        super(repo, ctx, query, (DeviceTypeFilter) query.getEntityFilter(), EntityType.DEVICE);
    }

    @Override
    protected String getEntityNameFilter(DeviceTypeFilter filter) {
        return filter.getDeviceNameFilter();
    }

    @Override
    protected List<String> getProfileNames(DeviceTypeFilter filter) {
        return filter.getDeviceTypes();
    }

    @Override
    protected EntityType getProfileEntityType() {
        return EntityType.DEVICE_PROFILE;
    }

}
