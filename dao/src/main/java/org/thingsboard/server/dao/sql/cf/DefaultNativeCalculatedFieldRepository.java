/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.dao.sql.cf;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.configuration.CalculatedFieldConfiguration;
import org.thingsboard.server.common.data.debug.DebugSettings;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Repository
@Slf4j
public class DefaultNativeCalculatedFieldRepository implements NativeCalculatedFieldRepository {

    private final String CF_COUNT_QUERY = "SELECT count(id) FROM calculated_field;";
    private final String CF_QUERY = "SELECT * FROM calculated_field ORDER BY created_time ASC LIMIT %s OFFSET %s";

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Override
    public PageData<CalculatedField> findCalculatedFields(Pageable pageable) {
        return transactionTemplate.execute(status -> {
            long startTs = System.currentTimeMillis();
            int totalElements = jdbcTemplate.queryForObject(CF_COUNT_QUERY, Collections.emptyMap(), Integer.class);
            log.debug("Count query took {} ms", System.currentTimeMillis() - startTs);
            startTs = System.currentTimeMillis();
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(String.format(CF_QUERY, pageable.getPageSize(), pageable.getOffset()), Collections.emptyMap());
            log.debug("Main query took {} ms", System.currentTimeMillis() - startTs);
            int totalPages = pageable.getPageSize() > 0 ? (int) Math.ceil((float) totalElements / pageable.getPageSize()) : 1;
            boolean hasNext = pageable.getPageSize() > 0 && totalElements > pageable.getOffset() + rows.size();
            var data = rows.stream().map(row -> {

                UUID id = (UUID) row.get("id");
                long createdTime = (long) row.get("created_time");
                UUID tenantId = (UUID) row.get("tenant_id");
                EntityType entityType = EntityType.valueOf((String) row.get("entity_type"));
                UUID entityId = (UUID) row.get("entity_id");
                CalculatedFieldType type = CalculatedFieldType.valueOf((String) row.get("type"));
                String name = (String) row.get("name");
                int configurationVersion = (int) row.get("configuration_version");
                JsonNode configuration = JacksonUtil.toJsonNode((String) row.get("configuration"));
                long version = row.get("version") != null ? (long) row.get("version") : 0;
                String debugSettings = (String) row.get("debug_settings");

                CalculatedField calculatedField = new CalculatedField();
                calculatedField.setId(new CalculatedFieldId(id));
                calculatedField.setCreatedTime(createdTime);
                calculatedField.setTenantId(TenantId.fromUUID(tenantId));
                calculatedField.setEntityId(EntityIdFactory.getByTypeAndUuid(entityType, entityId));
                calculatedField.setType(type);
                calculatedField.setName(name);
                calculatedField.setConfigurationVersion(configurationVersion);
                try {
                    calculatedField.setConfiguration(JacksonUtil.treeToValue(configuration, CalculatedFieldConfiguration.class));
                } catch (Exception e) {
                    log.error("Invalid configuration for CalculatedField [{}]. Skipping.", id, e);
                    return null;
                }
                calculatedField.setVersion(version);
                calculatedField.setDebugSettings(JacksonUtil.fromString(debugSettings, DebugSettings.class));

                return calculatedField;
            }).collect(Collectors.toList());
            return new PageData<>(data, totalPages, totalElements, hasNext);
        });
    }

}
