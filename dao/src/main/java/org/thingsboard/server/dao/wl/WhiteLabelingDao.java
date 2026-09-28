// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.wl;

import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.wl.WhiteLabeling;
import org.thingsboard.server.common.data.wl.WhiteLabelingType;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.WhiteLabelingCompositeKey;

import java.util.List;
import java.util.Set;

public interface WhiteLabelingDao extends TenantEntityDao<WhiteLabeling> {

    WhiteLabeling save(TenantId tenantId, WhiteLabeling whiteLabeling);

    WhiteLabeling findById(TenantId tenantId, WhiteLabelingCompositeKey key);

    WhiteLabeling findByDomainAndType(TenantId tenantId, String domain, WhiteLabelingType type);

    void removeById(TenantId tenantId, WhiteLabelingCompositeKey key);

    List<WhiteLabeling> findByTenantAndImageLink(TenantId tenantId, String imageUrl, int limit);

    List<WhiteLabeling> findByImageLink(String imageUrl, int limit);

    PageData<WhiteLabeling> findAllByType(PageLink pageLink, Set<WhiteLabelingType> types);

}
