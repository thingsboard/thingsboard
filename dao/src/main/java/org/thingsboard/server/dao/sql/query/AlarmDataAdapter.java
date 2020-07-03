/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatus;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.dao.model.ModelConstants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class AlarmDataAdapter {

    private final static ObjectMapper mapper = new ObjectMapper();

    public static PageData<AlarmData> createAlarmData(EntityDataPageLink pageLink,
                                                      List<Map<String, Object>> rows,
                                                      int totalElements, Collection<EntityId> orderedEntityIds) {
        Map<UUID, EntityId> entityIdMap = orderedEntityIds.stream().collect(Collectors.toMap(EntityId::getId, Function.identity()));
        int totalPages = pageLink.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageLink.getPageSize()) : 1;
        int startIndex = pageLink.getPageSize() * pageLink.getPage();
        boolean hasNext = pageLink.getPageSize() > 0 && totalElements > startIndex + rows.size();
        List<AlarmData> entitiesData = convertListToAlarmData(rows, entityIdMap);
        return new PageData<>(entitiesData, totalPages, totalElements, hasNext);
    }

    private static List<AlarmData> convertListToAlarmData(List<Map<String, Object>> result, Map<UUID, EntityId> entityIdMap) {
        return result.stream().map(tmp -> toEntityData(tmp, entityIdMap)).collect(Collectors.toList());
    }

    private static AlarmData toEntityData(Map<String, Object> row, Map<UUID, EntityId> entityIdMap) {
        Alarm alarm = new Alarm();
        alarm.setId(new AlarmId((UUID) row.get(ModelConstants.ID_PROPERTY)));
        alarm.setCreatedTime((long) row.get(ModelConstants.CREATED_TIME_PROPERTY));
        alarm.setAckTs((long) row.get(ModelConstants.ALARM_ACK_TS_PROPERTY));
        alarm.setClearTs((long) row.get(ModelConstants.ALARM_CLEAR_TS_PROPERTY));
        alarm.setStartTs((long) row.get(ModelConstants.ALARM_START_TS_PROPERTY));
        alarm.setEndTs((long) row.get(ModelConstants.ALARM_END_TS_PROPERTY));
        Object additionalInfo = row.get(ModelConstants.ADDITIONAL_INFO_PROPERTY);
        if (additionalInfo != null) {
            try {
                alarm.setDetails(mapper.readTree(additionalInfo.toString()));
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse json: {}", row.get(ModelConstants.ADDITIONAL_INFO_PROPERTY), e);
            }
        }
        EntityType originatorType = EntityType.values()[(int) row.get(ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY)];
        UUID originatorId = (UUID) row.get(ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY);
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(originatorType, originatorId));
        alarm.setPropagate((boolean) row.get(ModelConstants.ALARM_PROPAGATE_PROPERTY));
        alarm.setType(row.get(ModelConstants.ALARM_TYPE_PROPERTY).toString());
        alarm.setSeverity(AlarmSeverity.valueOf(row.get(ModelConstants.ALARM_SEVERITY_PROPERTY).toString()));
        alarm.setStatus(AlarmStatus.valueOf(row.get(ModelConstants.ALARM_STATUS_PROPERTY).toString()));
        alarm.setTenantId(new TenantId((UUID) row.get(ModelConstants.TENANT_ID_PROPERTY)));
        if (row.get(ModelConstants.ALARM_PROPAGATE_RELATION_TYPES) != null) {
            String propagateRelationTypes = row.get(ModelConstants.ALARM_PROPAGATE_RELATION_TYPES).toString();
            if (!StringUtils.isEmpty(propagateRelationTypes)) {
                alarm.setPropagateRelationTypes(Arrays.asList(propagateRelationTypes.split(",")));
            } else {
                alarm.setPropagateRelationTypes(Collections.emptyList());
            }
        } else {
            alarm.setPropagateRelationTypes(Collections.emptyList());
        }
        UUID entityUuid = (UUID) row.get(ModelConstants.ENTITY_ID_COLUMN);
        EntityId entityId = entityIdMap.get(entityUuid);
        Object originatorNameObj = row.get(ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY);
        String originatorName = originatorNameObj != null ? originatorNameObj.toString() : null;
        return new AlarmData(alarm, originatorName, entityId);
    }

}
