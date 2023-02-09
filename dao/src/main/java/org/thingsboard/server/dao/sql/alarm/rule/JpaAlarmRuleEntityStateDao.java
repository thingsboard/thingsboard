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
package org.thingsboard.server.dao.sql.alarm.rule;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.alarm.rule.AlarmRuleEntityState;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.RuleChainId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.alarm.rule.AlarmRuleEntityStateDao;
import org.thingsboard.server.dao.model.sql.AlarmRuleEntityStateEntity;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@Slf4j
@Component
@SqlDao
public class JpaAlarmRuleEntityStateDao implements AlarmRuleEntityStateDao {

    @Autowired
    private AlarmRuleEntityStateRepository alarmRuleEntityStateRepository;

    @Override
    public AlarmRuleEntityState saveAlarmRuleEntityState(TenantId tenantId, AlarmRuleEntityState alarmRuleEntityState){
        log.trace("Saving entity {}", alarmRuleEntityState);
        AlarmRuleEntityStateEntity saved = alarmRuleEntityStateRepository.save(new AlarmRuleEntityStateEntity(alarmRuleEntityState));
        return DaoUtil.getData(saved);
    }

    @Override
    public PageData<AlarmRuleEntityState> findAll(PageLink pageLink) {
        return DaoUtil.toPageData(alarmRuleEntityStateRepository.findAll(DaoUtil.toPageable(pageLink)));
    }

    @Override
    public List<AlarmRuleEntityState> findAllByIds(List<EntityId> entityIds) {
        List<UUID> uuidList = entityIds.stream().map(EntityId::getId).collect(Collectors.toList());
        return DaoUtil.convertDataList(alarmRuleEntityStateRepository.findAllByIds(uuidList));
    }

    @Override
    public void deleteByEntityId(TenantId tenantId, UUID id) {
        alarmRuleEntityStateRepository.deleteByEntityId(id);
    }

    @Override
    public List<JsonNode> findRuleNodeStatesByRuleChainIdAndType(DeviceProfileId deviceProfileId, RuleChainId ruleChainId, String type) {
        return alarmRuleEntityStateRepository.findRuleNodeStatesByRuleChainIdAndRuleNodeType(deviceProfileId.getId(), ruleChainId.getId(), type)
                .stream()
                .map(JacksonUtil::toJsonNode)
                .collect(Collectors.toList());
    }

}
