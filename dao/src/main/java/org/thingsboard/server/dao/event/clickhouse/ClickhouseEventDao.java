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
package org.thingsboard.server.dao.event.clickhouse;


import com.clickhouse.jdbc.ClickHouseDataSource;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Component;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.event.*;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.TimePageLink;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.event.EventDao;
import org.thingsboard.server.dao.model.sql.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * @author baigod
 * @version : ClickhouseEventDao.java, v 0.1 2024年05月31日 20:28 baigod Exp $
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "event.debug", value = "type", havingValue = "clickhouse")
public class ClickhouseEventDao implements EventDao {

    @Value("${clickhouse.url}")
    private String url;

    @Value("${clickhouse.cluster.enabled:false}")
    private boolean clusterEnabled;

    @Value("${clickhouse.cluster.name:default}")
    private String clusterName;

    @Getter
    private NamedParameterJdbcTemplate clickhouseJdbcTemplate;

    @PostConstruct
    public void init() throws SQLException {
        ClickHouseDataSource clickHouseDataSource = new ClickHouseDataSource(url);
        clickhouseJdbcTemplate = new NamedParameterJdbcTemplate(clickHouseDataSource);
    }

    @Override
    public ListenableFuture<Void> saveAsync(Event event) {
        return Futures.immediateFailedFuture(new ThingsboardException("saving events to clickhouse is not supported", ThingsboardErrorCode.GENERAL));
    }

    @Override
    public PageData<? extends Event> findEvents(UUID tenantId, UUID entityId, EventType eventType, TimePageLink pageLink) {
        switch (eventType) {
            case DEBUG_RULE_NODE:
                return findEvents(tenantId, entityId, eventType, pageLink, this::getRuleNodeDebugEventEntityRowMapper);
            case DEBUG_RULE_CHAIN:
                return findEvents(tenantId, entityId, eventType, pageLink, this::getRuleChainDebugEventEntityRowMapper);
            case LC_EVENT:
                return findEvents(tenantId, entityId, eventType, pageLink, this::getLifecycleEventEntityRowMapper);
            case ERROR:
                return findEvents(tenantId, entityId, eventType, pageLink, this::getErrorEventEntityRowMapper);
            case STATS:
                return findEvents(tenantId, entityId, eventType, pageLink, this::getStatisticsEventEntityRowMapper);
            default:
                throw new RuntimeException("Not supported event type: " + eventType);
        }
    }

    @Override
    public PageData<? extends Event> findEventByFilter(UUID tenantId, UUID entityId, EventFilter eventFilter, TimePageLink pageLink) {
        if (eventFilter.isNotEmpty()) {
            switch (eventFilter.getEventType()) {
                case DEBUG_RULE_NODE:
                    return findEventsByFilter(tenantId, entityId, (RuleNodeDebugEventFilter) eventFilter, pageLink,
                            this::getRuleNodeDebugEventEntityRowMapper);
                case DEBUG_RULE_CHAIN:
                    return findEventsByFilter(tenantId, entityId, (RuleChainDebugEventFilter) eventFilter, pageLink,
                            this::getRuleChainDebugEventEntityRowMapper);
                case LC_EVENT:
                    return findEventsByFilter(tenantId, entityId, (LifeCycleEventFilter) eventFilter, pageLink,
                            this::getLifecycleEventEntityRowMapper);
                case ERROR:
                    return findEventsByFilter(tenantId, entityId, (ErrorEventFilter) eventFilter, pageLink, this::getErrorEventEntityRowMapper);
                case STATS:
                    return findEventsByFilter(tenantId, entityId, (StatisticsEventFilter) eventFilter, pageLink,
                            this::getStatisticsEventEntityRowMapper);
                default:
                    throw new RuntimeException("Not supported event type: " + eventFilter.getEventType());
            }
        } else {
            return findEvents(tenantId, entityId, eventFilter.getEventType(), pageLink);
        }
    }

    @Override
    public List<? extends Event> findLatestEvents(UUID tenantId, UUID entityId, EventType eventType, int limit) {
        switch (eventType) {
            case DEBUG_RULE_NODE:
                return getLatestData(tenantId, entityId, eventType, limit, this::getRuleNodeDebugEventEntityRowMapper);
            case DEBUG_RULE_CHAIN:
                return getLatestData(tenantId, entityId, eventType, limit, this::getRuleChainDebugEventEntityRowMapper);
            case LC_EVENT:
                return getLatestData(tenantId, entityId, eventType, limit, this::getLifecycleEventEntityRowMapper);
            case ERROR:
                return getLatestData(tenantId, entityId, eventType, limit, this::getErrorEventEntityRowMapper);
            case STATS:
                return getLatestData(tenantId, entityId, eventType, limit, this::getStatisticsEventEntityRowMapper);
            default:
                throw new RuntimeException("Not supported event type: " + eventType);
        }
    }

    @Override
    public Event findLatestDebugRuleNodeInEvent(UUID tenantId, UUID entityId) {
        String sql = "SELECT * FROM rule_node_debug_event e " +
                "WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId AND e.e_type = 'IN' " +
                "ORDER BY e.ts DESC LIMIT 1";

        SqlParameterSource listParameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId);

        RuleNodeDebugEventEntity eventEntity = clickhouseJdbcTemplate.queryForObject(sql, listParameters, this.getRuleNodeDebugEventEntityRowMapper());

        return DaoUtil.getData(eventEntity);
    }

    @Override
    public void cleanupEvents(long regularEventExpTs, long debugEventExpTs, boolean cleanupDb) {
        //  We suggest setting the automatic expiration time of data through the clickhouse TTL parameter,
        //  so that there is no need to consume  thingsboard's own resources to call the cleaning process,
        //  and there will be no concurrency issues
    }

    @Override
    public void removeEvents(UUID tenantId, UUID entityId, Long startTime, Long endTime) {
        log.debug("[{}][{}] Remove events [{}-{}] ", tenantId, entityId, startTime, endTime);

        for (EventType eventType : EventType.values()) {
            String delSql = "ALTER TABLE "+eventType.getTable()+"  " +
                    (clusterEnabled ? "ON CLUSTER '" + clusterName + "'" : "") +
                    "DELETE  " +
                    "WHERE tenant_id = :tenantId  " +
                    "AND entity_id = :entityId " +
                    "AND (:startTime IS NULL OR ts >= :startTime)  " +
                    "AND (:endTime IS NULL OR ts <= :endTime)";

            MapSqlParameterSource parameters = new MapSqlParameterSource()
                    .addValue("tenantId", tenantId)
                    .addValue("entityId", entityId)
                    .addValue("startTime", startTime)
                    .addValue("endTime", endTime);

            int update = clickhouseJdbcTemplate.update(delSql, parameters);

            log.info("[{}] 删除 {} 条{}事件记录", entityId, update, eventType);
        }
    }

    @Override
    public void removeEvents(UUID tenantId, UUID entityId, EventFilter eventFilter, Long startTime, Long endTime) {
        if (eventFilter.isNotEmpty()) {
            switch (eventFilter.getEventType()) {
                case DEBUG_RULE_NODE:
                    removeEventsByFilter(tenantId, entityId, (RuleNodeDebugEventFilter) eventFilter, startTime, endTime);
                    break;
                case DEBUG_RULE_CHAIN:
                    removeEventsByFilter(tenantId, entityId, (RuleChainDebugEventFilter) eventFilter, startTime, endTime);
                    break;
                case LC_EVENT:
                    removeEventsByFilter(tenantId, entityId, (LifeCycleEventFilter) eventFilter, startTime, endTime);
                    break;
                case ERROR:
                    removeEventsByFilter(tenantId, entityId, (ErrorEventFilter) eventFilter, startTime, endTime);
                    break;
                case STATS:
                    removeEventsByFilter(tenantId, entityId, (StatisticsEventFilter) eventFilter, startTime, endTime);
                    break;
                default:
                    throw new RuntimeException("Not supported event type: " + eventFilter.getEventType());
            }
        } else {
            removeEvents(tenantId, entityId, startTime, endTime);
        }
    }


    private void removeEventsByFilter(UUID tenantId, UUID entityId, StatisticsEventFilter eventFilter, Long startTime, Long endTime) {
        String delSql = "ALTER TABLE stats_event " +
                (clusterEnabled ? "ON CLUSTER '" + clusterName + "'" : "") +
                "DELETE  " +
                "WHERE tenant_id = :tenantId  " +
                "AND  entity_id = :entityId " +
                "AND (:startTime IS NULL OR  ts >= :startTime) " +
                "AND (:endTime IS NULL OR  ts <= :endTime) " +
                "AND (:serviceId IS NULL OR  service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:minMessagesProcessed IS NULL OR  e_messages_processed >= :minMessagesProcessed) " +
                "AND (:maxMessagesProcessed IS NULL OR  e_messages_processed < :maxMessagesProcessed) " +
                "AND (:minErrorsOccurred IS NULL OR  e_errors_occurred >= :minErrorsOccurred) " +
                "AND (:maxErrorsOccurred IS NULL OR  e_errors_occurred < :maxErrorsOccurred)";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", startTime)
                .addValue("endTime", endTime)
                .addValue("serviceId", eventFilter.getServer())
                .addValue("minMessagesProcessed", eventFilter.getMinMessagesProcessed())
                .addValue("maxMessagesProcessed", eventFilter.getMaxMessagesProcessed())
                .addValue("minErrorsOccurred", eventFilter.getMinErrorsOccurred())
                .addValue("maxErrorsOccurred", eventFilter.getMaxErrorsOccurred());

        int update = clickhouseJdbcTemplate.update(delSql, parameters);

        log.info("[{}] 删除 {} 条stats_event事件记录", entityId, update);
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, ErrorEventFilter eventFilter, Long startTime, Long endTime) {
        String delSql = "ALTER TABLE error_event " +
                (clusterEnabled ? "ON CLUSTER '" + clusterName + "'" : "") +
                "DELETE  " +
                "WHERE tenant_id = :tenantId  " +
                "AND  entity_id = :entityId " +
                "AND (:startTime IS NULL OR  ts >= :startTime) " +
                "AND (:endTime IS NULL OR  ts <= :endTime) " +
                "AND (:serviceId IS NULL OR  service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:method IS NULL OR  e_method ILIKE concat('%', :method, '%')) " +
                "AND (:error IS NULL OR  e_error ILIKE concat('%', :error, '%'))";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", startTime)
                .addValue("endTime", endTime)
                .addValue("serviceId", eventFilter.getServer())
                .addValue("method", eventFilter.getMethod())
                .addValue("error", eventFilter.getErrorStr());

        int update = clickhouseJdbcTemplate.update(delSql, parameters);

        log.info("[{}] 删除 {} 条error_event事件记录", entityId, update);
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, LifeCycleEventFilter eventFilter, Long startTime, Long endTime) {
        boolean statusFilterEnabled = !StringUtils.isEmpty(eventFilter.getStatus());
        boolean statusFilter = statusFilterEnabled && eventFilter.getStatus().equalsIgnoreCase("Success");

        String delSql = "ALTER TABLE lc_event " +
                (clusterEnabled ? "ON CLUSTER '" + clusterName + "'" : "") +
                "DELETE  " +
                "WHERE tenant_id = :tenantId  " +
                "AND  entity_id = :entityId " +
                "AND (:startTime IS NULL OR  ts >= :startTime) " +
                "AND (:endTime IS NULL OR  ts <= :endTime) " +
                "AND (:serviceId IS NULL OR  service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:eventType IS NULL OR  e_type ILIKE concat('%', :eventType, '%')) " +
                "AND ((:statusFilterEnabled = FALSE) OR  e_success = :statusFilter) " +
                "AND (:error IS NULL OR  e_error ILIKE concat('%', :error, '%'))";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", startTime)
                .addValue("endTime", endTime)
                .addValue("serviceId", eventFilter.getServer())
                .addValue("eventType", eventFilter.getEvent())
                .addValue("statusFilterEnabled", statusFilterEnabled)
                .addValue("statusFilter", statusFilter)
                .addValue("error", eventFilter.getErrorStr());

        int update = clickhouseJdbcTemplate.update(delSql, parameters);

        log.info("[{}] 删除 {} 条lc_event事件记录", entityId, update);
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, RuleChainDebugEventFilter eventFilter, Long startTime, Long endTime) {
        String delSql = "ALTER TABLE rule_chain_debug_event " +
                (clusterEnabled ? "ON CLUSTER '" + clusterName + "'" : "") +
                "DELETE  " +
                "WHERE tenant_id = :tenantId  " +
                "AND  entity_id = :entityId " +
                "AND (:startTime IS NULL OR  ts >= :startTime) " +
                "AND (:endTime IS NULL OR  ts <= :endTime) " +
                "AND (:serviceId IS NULL OR  service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:message IS NULL OR  e_message ILIKE concat('%', :message, '%')) " +
                "AND ((:isError = FALSE) OR notEmpty( e_error) = 1) " +
                "AND (:error IS NULL OR  e_error ILIKE concat('%', :error, '%'))";


        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", startTime)
                .addValue("endTime", endTime)
                .addValue("serviceId", eventFilter.getServer())
                .addValue("message", eventFilter.getMessage())
                .addValue("isError", eventFilter.isError())
                .addValue("error", eventFilter.getErrorStr());

        int update = clickhouseJdbcTemplate.update(delSql, parameters);

        log.info("[{}] 删除 {} 条rule_chain_debug_event事件记录", entityId, update);
    }

    private void removeEventsByFilter(UUID tenantId, UUID entityId, RuleNodeDebugEventFilter eventFilter, Long startTime, Long endTime) {
        parseUUID(eventFilter.getEntityId(), "Entity Id");
        parseUUID(eventFilter.getMsgId(), "Message Id");

        String delSql = "ALTER TABLE rule_node_debug_event  " +
                (clusterEnabled ? "ON CLUSTER '" + clusterName + "'" : "") +
                "DELETE  " +
                "WHERE tenant_id = :tenantId  " +
                "AND  entity_id = :entityId " +
                "AND (:startTime IS NULL OR  ts >= :startTime) " +
                "AND (:endTime IS NULL OR  ts <= :endTime) " +
                "AND (:serviceId IS NULL OR  service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:eventType IS NULL OR  e_type ILIKE concat('%', :eventType, '%')) " +
                "AND (:eventEntityId IS NULL OR  e_entity_id = :eventEntityId) " +
                "AND (:eventEntityType IS NULL OR  e_entity_type ILIKE concat('%', :eventEntityType, '%')) " +
                "AND (:msgId IS NULL OR  e_msg_id = :msgId) " +
                "AND (:msgType IS NULL OR  e_msg_type ILIKE concat('%', :msgType, '%')) " +
                "AND (:relationType IS NULL OR  e_relation_type ILIKE concat('%', :relationType, '%')) " +
                "AND (:data IS NULL OR  e_data ILIKE concat('%', :data, '%')) " +
                "AND (:metadata IS NULL OR  e_metadata ILIKE concat('%', :metadata, '%')) " +
                "AND ((:isError = FALSE) OR notEmpty( e_error) =1) " +
                "AND (:error IS NULL OR  e_error ILIKE concat('%', :error, '%'))";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", startTime)
                .addValue("endTime", endTime)
                .addValue("serviceId", eventFilter.getServer())
                .addValue("eventType", eventFilter.getMsgDirectionType())
                .addValue("eventEntityId", eventFilter.getEntityId())
                .addValue("eventEntityType", eventFilter.getEntityType())
                .addValue("msgId", eventFilter.getMsgId())
                .addValue("msgType", eventFilter.getMsgType())
                .addValue("relationType", eventFilter.getRelationType())
                .addValue("data", eventFilter.getDataSearch())
                .addValue("metadata", eventFilter.getMetadataSearch())
                .addValue("isError", eventFilter.isError())
                .addValue("error", eventFilter.getErrorStr());

        int update = clickhouseJdbcTemplate.update(delSql, parameters);

        log.info("[{}] 删除 {} 条rule_node_debug_event事件记录", entityId, update);
    }

    @Override
    public void migrateEvents(long regularEventTs, long debugEventTs) {

    }

    private List<? extends Event> getLatestData(UUID tenantId,
                                                UUID entityId,
                                                EventType eventType,
                                                int limit,
                                                Supplier<RowMapper> rowMapperSupplier) {
        String sql = "SELECT * FROM " + eventType.getTable() + " e " +
                "WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId " +
                "ORDER BY e.ts DESC LIMIT :limit";


        SqlParameterSource listParameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("limit", limit)
                ;

        List list = clickhouseJdbcTemplate.query(sql, listParameters, rowMapperSupplier.get());

        return DaoUtil.convertDataList(list);
    }

    private PageData<? extends Event> findEventsByFilter(UUID tenantId,
                                                         UUID entityId,
                                                         RuleNodeDebugEventFilter eventFilter,
                                                         TimePageLink pageLink,
                                                         Supplier<RowMapper> rowMapperSupplier) {
        parseUUID(eventFilter.getEntityId(), "Entity Id");
        parseUUID(eventFilter.getMsgId(), "Message Id");

        String countSql = "SELECT count(*) FROM rule_node_debug_event e WHERE " +
                "e.tenant_id = :tenantId " +
                "AND e.entity_id = :entityId " +
                "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                "AND (:eventEntityId IS NULL OR e.e_entity_id = :eventEntityId) " +
                "AND (:eventEntityType IS NULL OR e.e_entity_type ILIKE concat('%', :eventEntityType, '%')) " +
                "AND (:msgId IS NULL OR e.e_msg_id = :msgId) " +
                "AND (:msgType IS NULL OR e.e_msg_type ILIKE concat('%', :msgType, '%')) " +
                "AND (:relationType IS NULL OR e.e_relation_type ILIKE concat('%', :relationType, '%')) " +
                "AND (:data IS NULL OR e.e_data ILIKE concat('%', :data, '%')) " +
                "AND (:metadata IS NULL OR e.e_metadata ILIKE concat('%', :metadata, '%')) " +
                "AND ((:isError = FALSE) OR notEmpty(e.e_error) =1) " +
                "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))";


        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", pageLink.getStartTime())
                .addValue("endTime", pageLink.getEndTime())
                .addValue("serviceId", eventFilter.getServer())
                .addValue("eventType", eventFilter.getMsgDirectionType())
                .addValue("eventEntityId", eventFilter.getEntityId())
                .addValue("eventEntityType", eventFilter.getEntityType())
                .addValue("msgId", eventFilter.getMsgId())
                .addValue("msgType", eventFilter.getMsgType())
                .addValue("relationType", eventFilter.getRelationType())
                .addValue("data", eventFilter.getDataSearch())
                .addValue("metadata", eventFilter.getMetadataSearch())
                .addValue("isError", eventFilter.isError())
                .addValue("error", eventFilter.getErrorStr());

        return queryPageData(pageLink, rowMapperSupplier, countSql, parameters);
    }

    private PageData<? extends Event> findEventsByFilter(UUID tenantId,
                                                         UUID entityId,
                                                         RuleChainDebugEventFilter eventFilter,
                                                         TimePageLink pageLink,
                                                         Supplier<RowMapper> rowMapperSupplier) {

        String countSql = "SELECT count(*) FROM rule_chain_debug_event e WHERE " +
                "e.tenant_id = :tenantId " +
                "AND e.entity_id = :entityId " +
                "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:message IS NULL OR e.e_message ILIKE concat('%', :message, '%')) " +
                "AND ((:isError = FALSE) OR notEmpty(e.e_error) = 1) " +
                "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))";


        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", pageLink.getStartTime())
                .addValue("endTime", pageLink.getEndTime())
                .addValue("serviceId", eventFilter.getServer())
                .addValue("message", eventFilter.getMessage())
                .addValue("isError", eventFilter.isError())
                .addValue("error", eventFilter.getErrorStr());

        return queryPageData(pageLink, rowMapperSupplier, countSql, parameters);
    }

    private PageData<? extends Event> findEventsByFilter(UUID tenantId,
                                                         UUID entityId,
                                                         LifeCycleEventFilter eventFilter,
                                                         TimePageLink pageLink,
                                                         Supplier<RowMapper> rowMapperSupplier) {
        boolean statusFilterEnabled = !StringUtils.isEmpty(eventFilter.getStatus());
        boolean statusFilter = statusFilterEnabled && eventFilter.getStatus().equalsIgnoreCase("Success");

        String countSql = "SELECT count(*) FROM lc_event e WHERE " +
                "e.tenant_id = :tenantId " +
                "AND e.entity_id = :entityId " +
                "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:eventType IS NULL OR e.e_type ILIKE concat('%', :eventType, '%')) " +
                "AND ((:statusFilterEnabled = FALSE) OR e.e_success = :statusFilter) " +
                "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", pageLink.getStartTime())
                .addValue("endTime", pageLink.getEndTime())
                .addValue("serviceId", eventFilter.getServer())
                .addValue("eventType", eventFilter.getEvent())
                .addValue("statusFilterEnabled", statusFilterEnabled)
                .addValue("statusFilter", statusFilter)
                .addValue("error", eventFilter.getErrorStr());

        return queryPageData(pageLink, rowMapperSupplier, countSql, parameters);
    }

    private PageData<? extends Event> findEventsByFilter(UUID tenantId,
                                                         UUID entityId,
                                                         ErrorEventFilter eventFilter,
                                                         TimePageLink pageLink,
                                                         Supplier<RowMapper> rowMapperSupplier) {

        String countSql = "SELECT count(*) FROM error_event e WHERE " +
                "e.tenant_id = :tenantId " +
                "AND e.entity_id = :entityId " +
                "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:method IS NULL OR e.e_method ILIKE concat('%', :method, '%')) " +
                "AND (:error IS NULL OR e.e_error ILIKE concat('%', :error, '%'))";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", pageLink.getStartTime())
                .addValue("endTime", pageLink.getEndTime())
                .addValue("serviceId", eventFilter.getServer())
                .addValue("method", eventFilter.getMethod())
                .addValue("error", eventFilter.getErrorStr());

        return queryPageData(pageLink, rowMapperSupplier, countSql, parameters);
    }

    private PageData<? extends Event> findEventsByFilter(UUID tenantId,
                                                         UUID entityId,
                                                         StatisticsEventFilter eventFilter,
                                                         TimePageLink pageLink,
                                                         Supplier<RowMapper> rowMapperSupplier) {

        String countSql = "SELECT count(*) FROM stats_event e WHERE " +
                "e.tenant_id = :tenantId " +
                "AND e.entity_id = :entityId " +
                "AND (:startTime IS NULL OR e.ts >= :startTime) " +
                "AND (:endTime IS NULL OR e.ts <= :endTime) " +
                "AND (:serviceId IS NULL OR e.service_id ILIKE concat('%', :serviceId, '%')) " +
                "AND (:minMessagesProcessed IS NULL OR e.e_messages_processed >= :minMessagesProcessed) " +
                "AND (:maxMessagesProcessed IS NULL OR e.e_messages_processed < :maxMessagesProcessed) " +
                "AND (:minErrorsOccurred IS NULL OR e.e_errors_occurred >= :minErrorsOccurred) " +
                "AND (:maxErrorsOccurred IS NULL OR e.e_errors_occurred < :maxErrorsOccurred)";

        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", pageLink.getStartTime())
                .addValue("endTime", pageLink.getEndTime())
                .addValue("serviceId", eventFilter.getServer())
                .addValue("minMessagesProcessed", eventFilter.getMinMessagesProcessed())
                .addValue("maxMessagesProcessed", eventFilter.getMaxMessagesProcessed())
                .addValue("minErrorsOccurred", eventFilter.getMinErrorsOccurred())
                .addValue("maxErrorsOccurred", eventFilter.getMaxErrorsOccurred());

        return queryPageData(pageLink, rowMapperSupplier, countSql, parameters);
    }

    private PageData<? extends Event> findEvents(UUID tenantId,
                                                 UUID entityId,
                                                 EventType eventType,
                                                 TimePageLink pageLink,
                                                 Supplier<RowMapper> rowMapperSupplier) {
        String countSql = "SELECT count(*) FROM " + eventType.getTable() + " e " +
                "WHERE e.tenant_id = :tenantId AND e.entity_id = :entityId " +
                "AND (:startTime IS NULL OR e.ts >= :startTime) AND (:endTime IS NULL OR e.ts <= :endTime) ";


        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("tenantId", tenantId)
                .addValue("entityId", entityId)
                .addValue("startTime", pageLink.getStartTime())
                .addValue("endTime", pageLink.getEndTime());

        return queryPageData(pageLink, rowMapperSupplier, countSql, parameters);
    }

    private PageData<? extends Event> queryPageData(TimePageLink pageLink, Supplier<RowMapper> rowMapperSupplier, String countSql,
                                                    MapSqlParameterSource parameters) {
        Integer total = clickhouseJdbcTemplate.queryForObject(countSql, parameters, Integer.class);

        Pageable pageable = DaoUtil.toPageable(pageLink, EventEntity.eventColumnMap);

        List list = new ArrayList<>();

        if (total > 0) {

            String sql = countSql.replace("count(*)", "*");

            // 拼接排序信息
            Sort sort = pageable.getSort();
            if (sort.isSorted()) {
                String orderBy = " ORDER BY ";
                List<String> orderByClauses = new ArrayList<>();
                for (Sort.Order order : sort) {
                    String direction = order.getDirection().name();
                    String property = order.getProperty();
                    orderByClauses.add(property + " " + direction);
                }
                orderBy += String.join(", ", orderByClauses);
                sql += orderBy;
            }

            // 分页信息
            sql += " LIMIT :offset,:limit";

            parameters
                    .addValue("offset", pageable.getOffset())
                    .addValue("limit", pageable.getPageSize());

            list = clickhouseJdbcTemplate.query(sql, parameters, rowMapperSupplier.get());
        }

        return DaoUtil.toPageData(new PageImpl<>(list, pageable, total));
    }

    private RowMapper<StatisticsEventEntity> getStatisticsEventEntityRowMapper() {
        return (rs, rowNum) -> {
            StatisticsEventEntity event = new StatisticsEventEntity();
            event.setMessagesProcessed(rs.getLong("e_messages_processed"));
            event.setErrorsOccurred(rs.getLong("e_errors_occurred"));

            setCommonEventFields(rs, event);

            return event;
        };
    }

    private RowMapper<ErrorEventEntity> getErrorEventEntityRowMapper() {
        return (rs, rowNum) -> {
            ErrorEventEntity event = new ErrorEventEntity();
            event.setMethod(rs.getString("e_method"));
            event.setError(rs.getString("e_error"));

            setCommonEventFields(rs, event);

            return event;
        };
    }

    private RowMapper<LifecycleEventEntity> getLifecycleEventEntityRowMapper() {
        return (rs, rowNum) -> {
            LifecycleEventEntity event = new LifecycleEventEntity();
            event.setEventType(rs.getString("e_type"));
            event.setSuccess(rs.getBoolean("e_success"));
            event.setError(rs.getString("e_error"));

            setCommonEventFields(rs, event);

            return event;
        };
    }

    private RowMapper<RuleChainDebugEventEntity> getRuleChainDebugEventEntityRowMapper() {
        return (rs, rowNum) -> {
            RuleChainDebugEventEntity event = new RuleChainDebugEventEntity();
            event.setMessage(rs.getString("e_message"));
            event.setError(rs.getString("e_error"));

            setCommonEventFields(rs, event);

            return event;
        };
    }

    private RowMapper<RuleNodeDebugEventEntity> getRuleNodeDebugEventEntityRowMapper() {
        return (rs, rowNum) -> {
            RuleNodeDebugEventEntity event = new RuleNodeDebugEventEntity();
            event.setEventType(rs.getString("e_type"));
            if (StringUtils.hasText(rs.getString("e_entity_id"))) {
                event.setEventEntityId(rs.getObject("e_entity_id", UUID.class));
            }
            event.setEventEntityType(rs.getString("e_entity_type"));
            event.setMsgId(rs.getObject("e_msg_id", UUID.class));
            event.setMsgType(rs.getString("e_msg_type"));
            event.setDataType(rs.getString("e_data_type"));
            event.setRelationType(rs.getString("e_relation_type"));
            event.setData(rs.getString("e_data"));
            event.setMetadata(rs.getString("e_metadata"));
            event.setError(rs.getString("e_error"));

            setCommonEventFields(rs, event);

            return event;
        };
    }

    private static void setCommonEventFields(ResultSet rs, EventEntity<? extends Event> event) throws SQLException {
        event.setUuid(rs.getObject("id", UUID.class));
        event.setCreatedTime(rs.getLong("ts"));
        event.setId(rs.getObject("id", UUID.class));
        event.setTenantId(rs.getObject("tenant_id", UUID.class));
        event.setEntityId(rs.getObject("entity_id", UUID.class));
        event.setServiceId(rs.getString("service_id"));
        event.setTs(rs.getLong("ts"));
    }

    private void parseUUID(String src, String paramName) {
        if (!StringUtils.isEmpty(src)) {
            try {
                UUID.fromString(src);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Failed to convert " + paramName + " to UUID!");
            }
        }
    }
}
