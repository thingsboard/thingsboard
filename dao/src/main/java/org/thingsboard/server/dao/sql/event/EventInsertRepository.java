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
package org.thingsboard.server.dao.sql.event;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.server.common.data.event.CalculatedFieldDebugEvent;
import org.thingsboard.server.common.data.event.ErrorEvent;
import org.thingsboard.server.common.data.event.Event;
import org.thingsboard.server.common.data.event.EventType;
import org.thingsboard.server.common.data.event.LifecycleEvent;
import org.thingsboard.server.common.data.event.RuleChainDebugEvent;
import org.thingsboard.server.common.data.event.RuleNodeDebugEvent;
import org.thingsboard.server.common.data.event.StatisticsEvent;
import org.thingsboard.server.dao.config.DefaultDataSource;
import org.thingsboard.server.dao.util.SqlDao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@DefaultDataSource
@Repository
@Transactional
@SqlDao
@RequiredArgsConstructor
public class EventInsertRepository {

    private static final ThreadLocal<Pattern> PATTERN_THREAD_LOCAL = ThreadLocal.withInitial(() -> Pattern.compile(String.valueOf(Character.MIN_VALUE)));

    private static final String EMPTY_STR = "";

    private final Map<EventType, String> insertStmtMap = new ConcurrentHashMap<>();

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${sql.remove_null_chars:true}")
    private boolean removeNullChars;

    @PostConstruct
    public void init() {
        insertStmtMap.put(EventType.ERROR, "INSERT INTO " + EventType.ERROR.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_method, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.LC_EVENT, "INSERT INTO " + EventType.LC_EVENT.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_type, e_success, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.STATS, "INSERT INTO " + EventType.STATS.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_messages_processed, e_errors_occurred) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.DEBUG_RULE_NODE, "INSERT INTO " + EventType.DEBUG_RULE_NODE.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_type, e_entity_id, e_entity_type, e_msg_id, e_msg_type, e_data_type, e_relation_type, e_data, e_metadata, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.DEBUG_RULE_CHAIN, "INSERT INTO " + EventType.DEBUG_RULE_CHAIN.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, e_message, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
        insertStmtMap.put(EventType.DEBUG_CALCULATED_FIELD, "INSERT INTO " + EventType.DEBUG_CALCULATED_FIELD.getTable() +
                " (id, tenant_id, ts, entity_id, service_id, cf_id, e_entity_id, e_entity_type, e_msg_id, e_msg_type, e_args, e_result, e_error) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT DO NOTHING;");
    }

    public void save(List<Event> entities) {
        Map<EventType, List<Event>> eventsByType = entities.stream().collect(Collectors.groupingBy(Event::getType, Collectors.toList()));
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                for (var entry : eventsByType.entrySet()) {
                    jdbcTemplate.batchUpdate(insertStmtMap.get(entry.getKey()), getStatementSetter(entry.getKey(), entry.getValue()));
                }
            }
        });
    }

    private BatchPreparedStatementSetter getStatementSetter(EventType eventType, List<Event> events) {
        switch (eventType) {
            case ERROR:
                return getErrorEventSetter(events);
            case LC_EVENT:
                return getLcEventSetter(events);
            case STATS:
                return getStatsEventSetter(events);
            case DEBUG_RULE_NODE:
                return getRuleNodeEventSetter(events);
            case DEBUG_RULE_CHAIN:
                return getRuleChainEventSetter(events);
            case DEBUG_CALCULATED_FIELD:
                return getCalculatedFieldEventSetter(events);
            default:
                throw new RuntimeException(eventType + " support is not implemented!");
        }
    }

    private BatchPreparedStatementSetter getErrorEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ErrorEvent event = (ErrorEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getMethod());
                safePutString(ps, 7, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    private BatchPreparedStatementSetter getLcEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                LifecycleEvent event = (LifecycleEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getLcEventType());
                ps.setBoolean(7, event.isSuccess());
                safePutString(ps, 8, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    private BatchPreparedStatementSetter getStatsEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                StatisticsEvent event = (StatisticsEvent) events.get(i);
                setCommonEventFields(ps, event);
                ps.setLong(6, event.getMessagesProcessed());
                ps.setLong(7, event.getErrorsOccurred());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    private BatchPreparedStatementSetter getRuleNodeEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RuleNodeDebugEvent event = (RuleNodeDebugEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getEventType());
                safePutUUID(ps, 7, event.getEventEntity() != null ? event.getEventEntity().getId() : null);
                safePutString(ps, 8, event.getEventEntity() != null ? event.getEventEntity().getEntityType().name() : null);
                safePutUUID(ps, 9, event.getMsgId());
                safePutString(ps, 10, event.getMsgType());
                safePutString(ps, 11, event.getDataType());
                safePutString(ps, 12, event.getRelationType());
                safePutString(ps, 13, event.getData());
                safePutString(ps, 14, event.getMetadata());
                safePutString(ps, 15, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    private BatchPreparedStatementSetter getRuleChainEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                RuleChainDebugEvent event = (RuleChainDebugEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutString(ps, 6, event.getMessage());
                safePutString(ps, 7, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    private BatchPreparedStatementSetter getCalculatedFieldEventSetter(List<Event> events) {
        return new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                CalculatedFieldDebugEvent event = (CalculatedFieldDebugEvent) events.get(i);
                setCommonEventFields(ps, event);
                safePutUUID(ps, 6, event.getCalculatedFieldId().getId());
                safePutUUID(ps, 7, event.getEventEntity() != null ? event.getEventEntity().getId() : null);
                safePutString(ps, 8, event.getEventEntity() != null ? event.getEventEntity().getEntityType().name() : null);
                safePutUUID(ps, 9, event.getMsgId());
                safePutString(ps, 10, event.getMsgType());
                safePutString(ps, 11, event.getArguments());
                safePutString(ps, 12, event.getResult());
                safePutString(ps, 13, event.getError());
            }

            @Override
            public int getBatchSize() {
                return events.size();
            }
        };
    }

    void safePutString(PreparedStatement ps, int parameterIdx, String value) throws SQLException {
        if (value != null) {
            ps.setString(parameterIdx, replaceNullChars(value));
        } else {
            ps.setNull(parameterIdx, Types.VARCHAR);
        }
    }

    void safePutUUID(PreparedStatement ps, int parameterIdx, UUID value) throws SQLException {
        if (value != null) {
            ps.setObject(parameterIdx, value);
        } else {
            ps.setNull(parameterIdx, Types.OTHER);
        }
    }

    private void setCommonEventFields(PreparedStatement ps, Event event) throws SQLException {
        ps.setObject(1, event.getId().getId());
        ps.setObject(2, event.getTenantId().getId());
        ps.setLong(3, event.getCreatedTime());
        ps.setObject(4, event.getEntityId());
        ps.setString(5, event.getServiceId());
    }

    private String replaceNullChars(String strValue) {
        if (removeNullChars && strValue != null) {
            return PATTERN_THREAD_LOCAL.get().matcher(strValue).replaceAll(EMPTY_STR);
        }
        return strValue;
    }

}
