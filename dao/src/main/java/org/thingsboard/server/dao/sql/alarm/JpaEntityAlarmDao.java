// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.sql.alarm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.util.SqlDao;

@Component
@SqlDao
public class JpaEntityAlarmDao implements TenantEntityDao<EntityAlarm> {

    @Autowired
    private EntityAlarmRepository entityAlarmRepository;

    @Override
    public PageData<EntityAlarm> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(entityAlarmRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink, "entityId", "alarmId")));
    }

}
