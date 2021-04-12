/**
 * Copyright © 2016-2021 The Thingsboard Authors
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
package org.thingsboard.server.service.install;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;
import org.thingsboard.server.common.data.ShortCustomerInfo;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.dashboard.DashboardService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by igor on 2/27/18.
 */
@Slf4j
public class DatabaseHelper {

    public static final CSVFormat CSV_DUMP_FORMAT = CSVFormat.DEFAULT.withNullString("\\N");

    public static final String DEVICE = "device";
    public static final String ENTITY_ID = "entity_id";
    public static final String TENANT_ID = "tenant_id";
    public static final String ENTITY_TYPE = "entity_type";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String SEARCH_TEXT = "search_text";
    public static final String ADDITIONAL_INFO = "additional_info";
    public static final String ASSET = "asset";
    public static final String DASHBOARD = "dashboard";
    public static final String ENTITY_VIEWS = "entity_views";
    public static final String ENTITY_VIEW = "entity_view";
    public static final String RULE_CHAIN = "rule_chain";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String TYPE = "type";
    public static final String NAME = "name";
    public static final String KEYS = "keys";
    public static final String START_TS = "start_ts";
    public static final String END_TS = "end_ts";
    public static final String ASSIGNED_CUSTOMERS = "assigned_customers";
    public static final String CONFIGURATION = "configuration";

    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static void upgradeTo40_assignDashboards(Path dashboardsDump, DashboardService dashboardService, boolean sql) throws Exception {
        JavaType assignedCustomersType =
                objectMapper.getTypeFactory().constructCollectionType(HashSet.class, ShortCustomerInfo.class);
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(dashboardsDump), CSV_DUMP_FORMAT.withFirstRecordAsHeader())) {
            csvParser.forEach(record -> {
                String customerIdString = record.get(CUSTOMER_ID);
                String assignedCustomersString = record.get(ASSIGNED_CUSTOMERS);
                DashboardId dashboardId = new DashboardId(toUUID(record.get(ID), sql));
                List<CustomerId> customerIds = new ArrayList<>();
                if (!StringUtils.isEmpty(assignedCustomersString)) {
                    try {
                        Set<ShortCustomerInfo> assignedCustomers = objectMapper.readValue(assignedCustomersString, assignedCustomersType);
                        assignedCustomers.forEach((customerInfo) -> {
                            CustomerId customerId = customerInfo.getCustomerId();
                            if (!customerId.isNullUid()) {
                                customerIds.add(customerId);
                            }
                        });
                    } catch (IOException e) {
                        log.error("Unable to parse assigned customers field", e);
                    }
                }
                if (!StringUtils.isEmpty(customerIdString)) {
                    CustomerId customerId = new CustomerId(toUUID(customerIdString, sql));
                    if (!customerId.isNullUid()) {
                        customerIds.add(customerId);
                    }
                }
                for (CustomerId customerId : customerIds) {
                    dashboardService.assignDashboardToCustomer(new TenantId(EntityId.NULL_UUID), dashboardId, customerId);
                }
            });
        }
    }

    private static UUID toUUID(String src, boolean sql) {
        if (sql) {
            return UUIDConverter.fromString(src);
        } else {
            return UUID.fromString(src);
        }
    }

}
