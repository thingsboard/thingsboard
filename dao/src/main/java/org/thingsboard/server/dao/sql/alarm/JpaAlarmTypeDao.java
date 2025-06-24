/**
 * Copyright © 2016-2024 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.dao.sql.alarm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.alarm.AlarmType;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.model.sql.AlarmTypeEntity;
import org.thingsboard.server.dao.util.SqlDao;

@Component
@SqlDao
public class JpaAlarmTypeDao implements TenantEntityDao<AlarmType> {

    @Autowired
    private AlarmTypeRepository alarmTypeRepository;

    @Override
    public PageData<AlarmType> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(alarmTypeRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink, "tenantId", "type")));
    }

    @Override
    public AlarmType save(TenantId tenantId, AlarmType alarmType) {
        return alarmTypeRepository.save(new AlarmTypeEntity(alarmType)).toData();
    }

    @Override
    public ObjectType getType() {
        return ObjectType.ALARM_TYPE;
    }

}
