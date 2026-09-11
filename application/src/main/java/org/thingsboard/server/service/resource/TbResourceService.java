// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.resource;

import org.thingsboard.server.common.data.Dashboard;
import org.thingsboard.server.common.data.ResourceExportData;
import org.thingsboard.server.common.data.TbResource;
import org.thingsboard.server.common.data.TbResourceDeleteResult;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.TbResourceInfo;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.lwm2m.LwM2mObject;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.widget.WidgetTypeDetails;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.List;

public interface TbResourceService {

    default TbResourceInfo save(TbResource entity) throws Exception {
        return save(entity, null);
    }

    TbResourceInfo save(TbResource entity, SecurityUser user) throws Exception;

    TbResourceDeleteResult delete(TbResourceInfo entity, boolean force, User user);

    List<LwM2mObject> findLwM2mObject(TenantId tenantId,
                                      String sortOrder,
                                      String sortProperty,
                                      String[] objectIds);

    List<LwM2mObject> findLwM2mObjectPage(TenantId tenantId,
                                          String sortProperty,
                                          String sortOrder,
                                          PageLink pageLink);

    List<ResourceExportData> exportResources(Dashboard dashboard, SecurityUser user) throws ThingsboardException;

    List<ResourceExportData> exportResources(WidgetTypeDetails widgetTypeDetails, SecurityUser user) throws ThingsboardException;

    void importResources(List<ResourceExportData> resources, SecurityUser user) throws Exception;

}
