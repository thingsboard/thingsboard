// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge.rpc.fetch;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EdgeUtils;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.edge.EdgeEvent;
import org.thingsboard.server.common.data.edge.EdgeEventActionType;
import org.thingsboard.server.common.data.edge.EdgeEventType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.menu.CustomMenu;
import org.thingsboard.server.common.data.menu.CustomMenuFilter;
import org.thingsboard.server.common.data.menu.CustomMenuInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.menu.CustomMenuService;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Slf4j
public class SystemCustomMenuEdgeEventFetcher implements EdgeEventFetcher {

    private final CustomMenuService customMenuService;

    @Override
    public PageLink getPageLink(int pageSize) {
        return new PageLink(pageSize);
    }

    @Override
    public PageData<EdgeEvent> fetchEdgeEvents(TenantId tenantId, Edge edge, PageLink pageLink) throws Exception {
        CustomMenuFilter customMenuFilter = CustomMenuFilter.builder()
                .tenantId(TenantId.SYS_TENANT_ID)
                .customerId(edge.getCustomerId())
                .scope(null)
                .assigneeType(null)
                .build();
        PageData<CustomMenuInfo> customMenuInfos = customMenuService.findCustomMenuInfos(tenantId, customMenuFilter, pageLink);
        List<EdgeEvent> events = new ArrayList<>();
        for (var cm : customMenuInfos.getData()) {
            CustomMenu customMenu = customMenuService.findCustomMenuById(cm.getTenantId(), cm.getId());
            events.add(EdgeUtils.constructEdgeEvent(tenantId, edge.getId(), EdgeEventType.CUSTOM_MENU, EdgeEventActionType.UPDATED, null, JacksonUtil.valueToTree(customMenu)));
        }
        return new PageData<>(events, customMenuInfos.getTotalPages(), customMenuInfos.getTotalElements(), customMenuInfos.hasNext());
    }

}
