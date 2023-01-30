/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.alarm.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.rule.AlarmRule;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleInfo;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleDao;
import org.thingsboard.server.dao.model.sql.AlarmRuleEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@SqlDao
public class JpaAlarmRuleDao extends JpaAbstractDao<AlarmRuleEntity, AlarmRule> implements AlarmRuleDao {

    @Autowired
    private AlarmRuleRepository alarmRuleRepository;

    @Override
    protected Class<AlarmRuleEntity> getEntityClass() {
        return AlarmRuleEntity.class;
    }

    @Override
    protected JpaRepository<AlarmRuleEntity, UUID> getRepository() {
        return alarmRuleRepository;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ALARM_RULE;
    }

    @Override
    public PageData<AlarmRuleInfo> findAlarmRuleInfosByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(alarmRuleRepository
                .findInfosByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }

    @Override
    public PageData<AlarmRule> findAlarmRulesByTenantId(UUID tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(alarmRuleRepository
                .findByTenantId(
                        tenantId,
                        Objects.toString(pageLink.getTextSearch(), ""),
                        DaoUtil.toPageable(pageLink)));
    }
}
