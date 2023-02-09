/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.dao.alarm.rule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleEntityState;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.service.Validator;

import java.util.List;

import static org.thingsboard.server.dao.service.Validator.validateId;

@Service
@Slf4j
public class BaseAlarmRuleEntityStateService extends AbstractEntityService implements AlarmRuleEntityStateService {

    @Autowired
    private AlarmRuleEntityStateDao alarmRuleEntityStateDao;

    @Override
    public PageData<AlarmRuleEntityState> findAll(PageLink pageLink) {
        return alarmRuleEntityStateDao.findAll(pageLink);
    }

    @Override
    public List<AlarmRuleEntityState> findAllByIds(List<EntityId> entityIds) {
        return alarmRuleEntityStateDao.findAllByIds(entityIds);
    }

    @Override
    public AlarmRuleEntityState save(TenantId tenantId, AlarmRuleEntityState entityState) {
        Validator.validateId(entityState.getTenantId(), "Incorrect tenant id.");
        Validator.validateEntityId(entityState.getEntityId(), "Incorrect entity id.");

        return alarmRuleEntityStateDao.saveAlarmRuleEntityState(tenantId, entityState);
    }

    @Override
    public void deleteByEntityId(TenantId tenantId, EntityId entityId) {
        log.trace("Executing removeByEntityId [{}]", entityId);
        validateId(entityId.getId(), "Incorrect entityId " + entityId);
        alarmRuleEntityStateDao.deleteByEntityId(tenantId, entityId.getId());
    }
}
