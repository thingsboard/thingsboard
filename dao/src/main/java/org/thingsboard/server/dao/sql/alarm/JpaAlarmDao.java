/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntitySubtype;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.alarm.Alarm;
import org.thingsboard.server.common.data.alarm.AlarmApiCallResult;
import org.thingsboard.server.common.data.alarm.AlarmAssignee;
import org.thingsboard.server.common.data.alarm.AlarmCreateOrUpdateActiveRequest;
import org.thingsboard.server.common.data.alarm.AlarmInfo;
import org.thingsboard.server.common.data.alarm.AlarmPropagationInfo;
import org.thingsboard.server.common.data.alarm.AlarmQuery;
import org.thingsboard.server.common.data.alarm.AlarmQueryV2;
import org.thingsboard.server.common.data.alarm.AlarmSeverity;
import org.thingsboard.server.common.data.alarm.AlarmStatusFilter;
import org.thingsboard.server.common.data.alarm.AlarmUpdateRequest;
import org.thingsboard.server.common.data.alarm.EntityAlarm;
import org.thingsboard.server.common.data.id.AlarmId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.page.SortOrder;
import org.thingsboard.server.common.data.query.AlarmCountQuery;
import org.thingsboard.server.common.data.query.AlarmData;
import org.thingsboard.server.common.data.query.AlarmDataQuery;
import org.thingsboard.server.common.data.query.OriginatorAlarmFilter;
import org.thingsboard.server.common.data.util.TbPair;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.TenantEntityDao;
import org.thingsboard.server.dao.alarm.AlarmDao;
import org.thingsboard.server.dao.model.ModelConstants;
import org.thingsboard.server.dao.model.sql.AlarmEntity;
import org.thingsboard.server.dao.model.sql.EntityAlarmEntity;
import org.thingsboard.server.dao.sql.JpaAbstractDao;
import org.thingsboard.server.dao.sql.query.AlarmQueryRepository;
import org.thingsboard.server.dao.util.SqlDao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.thingsboard.server.common.data.page.SortOrder.Direction.ASC;
import static org.thingsboard.server.dao.DaoUtil.convertTenantEntityTypesToDto;
import static org.thingsboard.server.dao.DaoUtil.toPageable;

@Slf4j
@Component
@SqlDao
public class JpaAlarmDao extends JpaAbstractDao<AlarmEntity, Alarm> implements AlarmDao, TenantEntityDao<Alarm> {

    @Autowired
    private AlarmRepository alarmRepository;

    @Autowired
    private AlarmQueryRepository alarmQueryRepository;

    @Autowired
    private EntityAlarmRepository entityAlarmRepository;

    @Override
    protected Class<AlarmEntity> getEntityClass() {
        return AlarmEntity.class;
    }

    @Override
    protected JpaRepository<AlarmEntity, UUID> getRepository() {
        return alarmRepository;
    }

    @Override
    public Alarm findLatestByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        List<AlarmEntity> latest = alarmRepository.findLatestByOriginatorAndType(
                originator.getId(),
                type,
                PageRequest.of(0, 1));
        return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
    }

    @Override
    public Alarm findLatestActiveByOriginatorAndType(TenantId tenantId, EntityId originator, String type) {
        List<AlarmEntity> latest = alarmRepository.findLatestActiveByOriginatorAndType(
                originator.getId(),
                type,
                PageRequest.of(0, 1));
        return latest.isEmpty() ? null : DaoUtil.getData(latest.get(0));
    }

    @Override
    public FluentFuture<Alarm> findLatestActiveByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type) {
        return FluentFuture.from(service.submit(() -> findLatestActiveByOriginatorAndType(tenantId, originator, type)));
    }

    @Override
    public ListenableFuture<Alarm> findLatestByOriginatorAndTypeAsync(TenantId tenantId, EntityId originator, String type) {
        return service.submit(() -> findLatestByOriginatorAndType(tenantId, originator, type));
    }

    @Override
    public Alarm findAlarmById(TenantId tenantId, UUID key) {
        return findById(tenantId, key);
    }

    @Override
    public AlarmInfo findAlarmInfoById(TenantId tenantId, UUID key) {
        return DaoUtil.getData(alarmRepository.findAlarmInfoById(tenantId.getId(), key));
    }

    @Override
    public ListenableFuture<Alarm> findAlarmByIdAsync(TenantId tenantId, UUID key) {
        return findByIdAsync(tenantId, key);
    }

    @Override
    public PageData<AlarmInfo> findAlarms(TenantId tenantId, AlarmQuery query) {
        log.trace("Try to find alarms by entity [{}], status [{}] and pageLink [{}]", query.getAffectedEntityId(), query.getStatus(), query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        AlarmStatusFilter asf = AlarmStatusFilter.from(query);
        if (affectedEntity != null) {
            return DaoUtil.toPageData(
                    alarmRepository.findAlarms(
                            tenantId.getId(),
                            affectedEntity.getId(),
                            affectedEntity.getEntityType().name(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            asf.hasClearFilter(),
                            asf.hasClearFilter() && asf.getClearFilter(),
                            asf.hasAckFilter(),
                            asf.hasAckFilter() && asf.getAckFilter(),
                            DaoUtil.getId(query.getAssigneeId()),
                            query.getPageLink().getTextSearch(),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        } else {
            return DaoUtil.toPageData(
                    alarmRepository.findAllAlarms(
                            tenantId.getId(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            asf.hasClearFilter(),
                            asf.hasClearFilter() && asf.getClearFilter(),
                            asf.hasAckFilter(),
                            asf.hasAckFilter() && asf.getAckFilter(),
                            DaoUtil.getId(query.getAssigneeId()),
                            query.getPageLink().getTextSearch(),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        }
    }

    @Override
    public PageData<AlarmInfo> findCustomerAlarms(TenantId tenantId, CustomerId customerId, AlarmQuery query) {
        log.trace("Try to find customer alarms by status [{}] and pageLink [{}]", query.getStatus(), query.getPageLink());
        AlarmStatusFilter asf = AlarmStatusFilter.from(query);
        return DaoUtil.toPageData(
                alarmRepository.findCustomerAlarms(
                        tenantId.getId(),
                        customerId.getId(),
                        query.getPageLink().getStartTime(),
                        query.getPageLink().getEndTime(),
                        asf.hasClearFilter(),
                        asf.hasClearFilter() && asf.getClearFilter(),
                        asf.hasAckFilter(),
                        asf.hasAckFilter() && asf.getAckFilter(),
                        DaoUtil.getId(query.getAssigneeId()),
                        query.getPageLink().getTextSearch(),
                        DaoUtil.toPageable(query.getPageLink())
                )
        );
    }

    @Override
    public PageData<AlarmInfo> findAlarmsV2(TenantId tenantId, AlarmQueryV2 query) {
        log.trace("Try to find alarms by entity [{}], query [{}] and pageLink [{}]", query.getAffectedEntityId(), query, query.getPageLink());
        EntityId affectedEntity = query.getAffectedEntityId();
        List<String> typeList = query.getTypeList() != null && !query.getTypeList().isEmpty() ? query.getTypeList() : null;
        List<AlarmSeverity> severityList = query.getSeverityList() != null && !query.getSeverityList().isEmpty() ? query.getSeverityList() : null;
        AlarmStatusFilter asf = AlarmStatusFilter.from(query.getStatusList());
        if (affectedEntity != null) {
            return DaoUtil.toPageData(
                    alarmRepository.findAlarmsV2(
                            tenantId.getId(),
                            affectedEntity.getId(),
                            affectedEntity.getEntityType().name(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            typeList,
                            severityList,
                            asf.hasClearFilter(),
                            asf.hasClearFilter() && asf.getClearFilter(),
                            asf.hasAckFilter(),
                            asf.hasAckFilter() && asf.getAckFilter(),
                            DaoUtil.getId(query.getAssigneeId()),
                            query.getPageLink().getTextSearch(),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        } else {
            return DaoUtil.toPageData(
                    alarmRepository.findAllAlarmsV2(
                            tenantId.getId(),
                            query.getPageLink().getStartTime(),
                            query.getPageLink().getEndTime(),
                            typeList,
                            severityList,
                            asf.hasClearFilter(),
                            asf.hasClearFilter() && asf.getClearFilter(),
                            asf.hasAckFilter(),
                            asf.hasAckFilter() && asf.getAckFilter(),
                            DaoUtil.getId(query.getAssigneeId()),
                            query.getPageLink().getTextSearch(),
                            DaoUtil.toPageable(query.getPageLink())
                    )
            );
        }
    }

    @Override
    public PageData<AlarmInfo> findCustomerAlarmsV2(TenantId tenantId, CustomerId customerId, AlarmQueryV2 query) {
        log.trace("Try to find customer alarms by query [{}] and pageLink [{}]", query, query.getPageLink());
        List<String> typeList = query.getTypeList() != null && !query.getTypeList().isEmpty() ? query.getTypeList() : null;
        List<AlarmSeverity> severityList = query.getSeverityList() != null && !query.getSeverityList().isEmpty() ? query.getSeverityList() : null;
        AlarmStatusFilter asf = AlarmStatusFilter.from(query.getStatusList());
        return DaoUtil.toPageData(
                alarmRepository.findCustomerAlarmsV2(
                        tenantId.getId(),
                        customerId.getId(),
                        query.getPageLink().getStartTime(),
                        query.getPageLink().getEndTime(),
                        typeList,
                        severityList,
                        asf.hasClearFilter(),
                        asf.hasClearFilter() && asf.getClearFilter(),
                        asf.hasAckFilter(),
                        asf.hasAckFilter() && asf.getAckFilter(),
                        DaoUtil.getId(query.getAssigneeId()),
                        query.getPageLink().getTextSearch(),
                        DaoUtil.toPageable(query.getPageLink())
                )
        );
    }

    @Override
    public PageData<AlarmData> findAlarmDataByQueryForEntities(TenantId tenantId, AlarmDataQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmQueryRepository.findAlarmDataByQueryForEntities(tenantId, query, orderedEntityIds);
    }

    @Override
    public Set<AlarmSeverity> findAlarmSeverities(TenantId tenantId, EntityId entityId, AlarmStatusFilter asf, String assigneeId) {
        return alarmRepository.findAlarmSeverities(tenantId.getId(), entityId.getId(), entityId.getEntityType().name(),
                asf.hasClearFilter(),
                asf.hasClearFilter() && asf.getClearFilter(),
                asf.hasAckFilter(),
                asf.hasAckFilter() && asf.getAckFilter(),
                StringUtils.isNotBlank(assigneeId) ? UUID.fromString(assigneeId) : null);
    }

    @Override
    public PageData<AlarmId> findAlarmsIdsByEndTsBeforeAndTenantId(Long time, TenantId tenantId, PageLink pageLink) {
        return DaoUtil.pageToPageData(alarmRepository.findAlarmsIdsByEndTsBeforeAndTenantId(time, tenantId.getId(), DaoUtil.toPageable(pageLink)))
                .mapData(AlarmId::new);
    }

    @Override
    public PageData<TbPair<UUID, Long>> findAlarmIdsByAssigneeId(TenantId tenantId, UserId userId, long createdTimeOffset, AlarmId idOffset, int limit) {
        Slice<TbPair<UUID, Long>> result;
        Pageable pageRequest = toPageable(new PageLink(limit), List.of(SortOrder.of("createdTime", ASC), SortOrder.of("id", ASC)));
        if (idOffset == null) {
            result = alarmRepository.findAlarmIdsByAssigneeId(tenantId.getId(), userId.getId(), pageRequest);
        } else {
            result = alarmRepository.findAlarmIdsByAssigneeId(tenantId.getId(), userId.getId(), createdTimeOffset, idOffset.getId(), pageRequest);
        }
        return DaoUtil.pageToPageData(result);
    }

    @Override
    public PageData<TbPair<UUID, Long>> findAlarmIdsByOriginatorId(TenantId tenantId, EntityId originatorId, long createdTimeOffset, AlarmId idOffset, int limit) {
        Slice<TbPair<UUID, Long>> result;
        Pageable pageRequest = toPageable(new PageLink(limit), List.of(SortOrder.of("createdTime", ASC), SortOrder.of("id", ASC)));
        if (idOffset == null) {
            result = alarmRepository.findAlarmIdsByOriginatorId(originatorId.getId(), pageRequest);
        } else {
            result = alarmRepository.findAlarmIdsByOriginatorId(originatorId.getId(), createdTimeOffset, idOffset.getId(), pageRequest);
        }
        return DaoUtil.pageToPageData(result);
    }

    @Override
    public void createEntityAlarmRecord(EntityAlarm entityAlarm) {
        log.debug("Saving entity {}", entityAlarm);
        entityAlarmRepository.save(new EntityAlarmEntity(entityAlarm));
    }

    @Override
    public List<EntityAlarm> findEntityAlarmRecords(TenantId tenantId, AlarmId id) {
        log.trace("[{}] Try to find entity alarm records using [{}]", tenantId, id);
        return DaoUtil.convertDataList(entityAlarmRepository.findAllByAlarmId(id.getId()));
    }

    @Override
    public List<EntityAlarm> findEntityAlarmRecordsByEntityId(TenantId tenantId, EntityId entityId) {
        return DaoUtil.convertDataList(entityAlarmRepository.findAllByEntityId(entityId.getId()));
    }

    @Override
    public int deleteEntityAlarmRecords(TenantId tenantId, EntityId entityId) {
        return entityAlarmRepository.deleteByEntityId(entityId.getId());
    }

    @Override
    public void deleteEntityAlarmRecordsByTenantId(TenantId tenantId) {
        entityAlarmRepository.deleteByTenantId(tenantId.getId());
    }

    @Override
    public AlarmApiCallResult createOrUpdateActiveAlarm(AlarmCreateOrUpdateActiveRequest request, boolean alarmCreationEnabled) {
        UUID tenantUUID = request.getTenantId().getId();
        log.debug("[{}] createOrUpdateActiveAlarm [{}] {}", tenantUUID, alarmCreationEnabled, request);

        AlarmPropagationInfo ap = getSafePropagationInfo(request.getPropagation());
        return toAlarmApiResult(alarmRepository.createOrUpdateActiveAlarm(
                tenantUUID,
                request.getCustomerId() != null ? request.getCustomerId().getId() : CustomerId.NULL_UUID,
                request.getEdgeAlarmId() != null ? request.getEdgeAlarmId().getId() : UUID.randomUUID(),
                System.currentTimeMillis(),
                request.getOriginator().getId(),
                request.getOriginator().getEntityType().ordinal(),
                request.getType(),
                request.getSeverity().name(),
                request.getStartTs(), request.getEndTs(),
                getDetailsAsString(request.getDetails()),
                ap.isPropagate(),
                ap.isPropagateToOwner(),
                ap.isPropagateToTenant(),
                getPropagationTypes(ap),
                alarmCreationEnabled
        ));
    }

    @Override
    public AlarmApiCallResult updateAlarm(AlarmUpdateRequest request) {
        UUID tenantUUID = request.getTenantId().getId();
        UUID alarmUUID = request.getAlarmId().getId();
        log.debug("[{}][{}] updateAlarm {}", tenantUUID, alarmUUID, request);

        AlarmPropagationInfo ap = getSafePropagationInfo(request.getPropagation());
        return toAlarmApiResult(alarmRepository.updateAlarm(
                tenantUUID,
                alarmUUID,
                request.getSeverity().name(),
                request.getStartTs(), request.getEndTs(),
                getDetailsAsString(request.getDetails()),
                ap.isPropagate(),
                ap.isPropagateToOwner(),
                ap.isPropagateToTenant(),
                getPropagationTypes(ap)
        ));
    }

    @Override
    public AlarmApiCallResult acknowledgeAlarm(TenantId tenantId, AlarmId id, long ackTs) {
        log.debug("[{}][{}] acknowledgeAlarm [{}]", tenantId, id, ackTs);
        return toAlarmApiResult(alarmRepository.acknowledgeAlarm(tenantId.getId(), id.getId(), ackTs));
    }

    @Override
    public AlarmApiCallResult clearAlarm(TenantId tenantId, AlarmId id, long clearTs, JsonNode details) {
        log.debug("[{}][{}] clearAlarm [{}]", tenantId, id, clearTs);
        return toAlarmApiResult(alarmRepository.clearAlarm(tenantId.getId(), id.getId(), clearTs, details != null ? getDetailsAsString(details) : null));
    }

    @Override
    public AlarmApiCallResult assignAlarm(TenantId tenantId, AlarmId id, UserId assigneeId, long assignTime) {
        return toAlarmApiResult(alarmRepository.assignAlarm(tenantId.getId(), id.getId(), assigneeId.getId(), assignTime));
    }

    @Override
    public AlarmApiCallResult unassignAlarm(TenantId tenantId, AlarmId id, long unassignTime) {
        return toAlarmApiResult(alarmRepository.unassignAlarm(tenantId.getId(), id.getId(), unassignTime));
    }

    @Override
    public long countAlarmsByQuery(TenantId tenantId, CustomerId customerId, AlarmCountQuery query, Collection<EntityId> orderedEntityIds) {
        return alarmQueryRepository.countAlarmsByQuery(tenantId, customerId, query, orderedEntityIds);
    }

    @Override
    public PageData<EntitySubtype> findTenantAlarmTypes(UUID tenantId, PageLink pageLink) {
        Page<String> page = alarmRepository.findTenantAlarmTypes(tenantId, Objects.toString(pageLink.getTextSearch(), ""), toPageable(pageLink, false));
        if (page.isEmpty()) {
            return PageData.emptyPageData();
        }

        List<EntitySubtype> data = convertTenantEntityTypesToDto(tenantId, EntityType.ALARM, page.getContent());
        return new PageData<>(data, page.getTotalPages(), page.getTotalElements(), page.hasNext());
    }

    @Override
    public boolean removeAlarmTypesIfNoAlarmsPresent(UUID tenantId, Set<String> types) {
        return alarmRepository.deleteTypeIfNoAlarmsExist(tenantId, types) > 0;
    }

    @Override
    public List<UUID> findActiveOriginatorAlarms(TenantId tenantId, OriginatorAlarmFilter filter, int limit) {
        return alarmRepository.findActiveOriginatorAlarms(filter.getOriginatorId().getId(),
                filter.getTypeList(), filter.getSeverityList() != null ? filter.getSeverityList().stream().map(Enum::name).toList() : null,
                limit);
    }

    private static String getPropagationTypes(AlarmPropagationInfo ap) {
        String propagateRelationTypes;
        if (!CollectionUtils.isEmpty(ap.getPropagateRelationTypes())) {
            propagateRelationTypes = String.join(",", ap.getPropagateRelationTypes());
        } else {
            propagateRelationTypes = "";
        }
        return propagateRelationTypes;
    }

    private static AlarmPropagationInfo getSafePropagationInfo(AlarmPropagationInfo ap) {
        return ap != null ? ap : AlarmPropagationInfo.EMPTY;
    }

    private static String getDetailsAsString(JsonNode details) {
        var detailsStr = JacksonUtil.toString(details);
        if (StringUtils.isEmpty(detailsStr)) {
            detailsStr = "{}";
        }
        return detailsStr;
    }

    private AlarmApiCallResult toAlarmApiResult(String str) {
        var json = JacksonUtil.toJsonNode(str);
        var result = AlarmApiCallResult.builder();
        boolean success = json.get("success").asBoolean();
        result.successful(success);
        if (success) {
            boolean modified = false;
            boolean created = false;
            boolean cleared = false;
            if (json.has("modified")) {
                modified = json.get("modified").asBoolean();
            }

            if (json.has("created")) {
                created = json.get("created").asBoolean();
            }

            if (json.has("cleared")) {
                cleared = json.get("cleared").asBoolean();
            }
            result.created(created);
            result.cleared(cleared);
            result.modified(created || cleared || modified);
            if (json.has("alarm") && !json.get("alarm").isNull()) {
                result.alarm(toAlarmInfo(json.get("alarm")));
            }
            if (json.has("old") && !json.get("old").isNull()) {
                result.old(toAlarm(json.get("old")));
            }
        }
        return result.build();
    }

    private AlarmInfo toAlarmInfo(JsonNode json) {
        AlarmInfo alarmInfo = new AlarmInfo(toAlarm(json));
        getSafe(json, ModelConstants.ALARM_ORIGINATOR_NAME_PROPERTY).ifPresent(alarmInfo::setOriginatorName);
        getSafe(json, ModelConstants.ALARM_ORIGINATOR_LABEL_PROPERTY).ifPresent(alarmInfo::setOriginatorLabel);
        if (alarmInfo.getAssigneeId() != null) {
            var assigneeBuilder = AlarmAssignee.builder().id(alarmInfo.getAssigneeId());
            getSafe(json, ModelConstants.ALARM_ASSIGNEE_FIRST_NAME_PROPERTY).ifPresent(assigneeBuilder::firstName);
            getSafe(json, ModelConstants.ALARM_ASSIGNEE_LAST_NAME_PROPERTY).ifPresent(assigneeBuilder::lastName);
            getSafe(json, ModelConstants.ALARM_ASSIGNEE_EMAIL_PROPERTY).ifPresent(assigneeBuilder::email);
            alarmInfo.setAssignee(assigneeBuilder.build());
        }
        return alarmInfo;
    }

    private Alarm toAlarm(JsonNode json) {
        Alarm alarm = new Alarm(new AlarmId(UUID.fromString(json.get(ModelConstants.ID_PROPERTY).asText())));
        alarm.setCreatedTime(json.get(ModelConstants.CREATED_TIME_PROPERTY).asLong());
        getSafe(json, ModelConstants.TENANT_ID_COLUMN).ifPresent(s -> alarm.setTenantId(TenantId.fromUUID(UUID.fromString(s))));
        getSafe(json, ModelConstants.CUSTOMER_ID_PROPERTY).ifPresent(s -> alarm.setCustomerId(new CustomerId(UUID.fromString(s))));
        getSafe(json, ModelConstants.ASSIGNEE_ID_PROPERTY).ifPresent(s -> alarm.setAssigneeId(new UserId(UUID.fromString(s))));
        alarm.setOriginator(EntityIdFactory.getByTypeAndUuid(
                json.get(ModelConstants.ALARM_ORIGINATOR_TYPE_PROPERTY).asInt(),
                json.get(ModelConstants.ALARM_ORIGINATOR_ID_PROPERTY).asText()));
        getSafe(json, ModelConstants.ALARM_TYPE_PROPERTY).ifPresent(alarm::setType);
        getSafe(json, ModelConstants.ALARM_SEVERITY_PROPERTY).map(AlarmSeverity::valueOf).ifPresent(alarm::setSeverity);
        alarm.setAcknowledged(json.get(ModelConstants.ALARM_ACKNOWLEDGED_PROPERTY).asBoolean());
        alarm.setCleared(json.get(ModelConstants.ALARM_CLEARED_PROPERTY).asBoolean());
        alarm.setPropagate(json.get(ModelConstants.ALARM_PROPAGATE_PROPERTY).asBoolean());
        alarm.setPropagateToOwner(json.get(ModelConstants.ALARM_PROPAGATE_TO_OWNER_PROPERTY).asBoolean());
        alarm.setPropagateToTenant(json.get(ModelConstants.ALARM_PROPAGATE_TO_TENANT_PROPERTY).asBoolean());
        alarm.setStartTs(json.get(ModelConstants.ALARM_START_TS_PROPERTY).asLong());
        alarm.setEndTs(json.get(ModelConstants.ALARM_END_TS_PROPERTY).asLong());
        alarm.setAckTs(json.get(ModelConstants.ALARM_ACK_TS_PROPERTY).asLong());
        alarm.setClearTs(json.get(ModelConstants.ALARM_CLEAR_TS_PROPERTY).asLong());
        alarm.setAssignTs(json.get(ModelConstants.ALARM_ASSIGN_TS_PROPERTY).asLong());
        getSafe(json, ModelConstants.ALARM_DETAILS_PROPERTY).map(JacksonUtil::toJsonNode).ifPresent(alarm::setDetails);
        alarm.setPropagateRelationTypes(getSafe(json, ModelConstants.ALARM_PROPAGATE_RELATION_TYPES).filter(StringUtils::isNoneEmpty)
                .map(s -> Arrays.asList(s.split(","))).orElse(Collections.emptyList()));
        return alarm;
    }

    private static Optional<String> getSafe(JsonNode json, String fieldName) {
        if (json.has(fieldName)) {
            var element = json.get(fieldName);
            if (element.isNull() || !element.isTextual()) {
                return Optional.empty();
            } else {
                return Optional.of(element.asText());
            }
        } else {
            return Optional.empty();
        }
    }

    @Override
    public PageData<Alarm> findAllByTenantId(TenantId tenantId, PageLink pageLink) {
        return DaoUtil.toPageData(alarmRepository.findByTenantId(tenantId.getId(), DaoUtil.toPageable(pageLink)));
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ALARM;
    }

}
