/**
 * Copyright © 2016-2017 The Thingsboard Authors
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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.JsonNode;
import org.thingsboard.server.common.data.UUIDConverter;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DashboardId;
import org.thingsboard.server.dao.dashboard.DashboardService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by igor on 2/27/18.
 */
@Slf4j
public class DatabaseHelper {

    public static final CSVFormat CSV_DUMP_FORMAT = CSVFormat.DEFAULT.withNullString("\\N");

    public static final String DEVICE = "device";
    public static final String TENANT_ID = "tenant_id";
    public static final String CUSTOMER_ID = "customer_id";
    public static final String SEARCH_TEXT = "search_text";
    public static final String ADDITIONAL_INFO = "additional_info";
    public static final String ASSET = "asset";
    public static final String DASHBOARD = "dashboard";
    public static final String ID = "id";
    public static final String TITLE = "title";
    public static final String ASSIGNED_CUSTOMERS = "assigned_customers";
    public static final String CONFIGURATION = "configuration";

    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static void upgradeTo40_assignDashboards(Path dashboardsDump, DashboardService dashboardService, boolean sql) throws Exception {
        String[] columns = new String[]{ID, TENANT_ID, CUSTOMER_ID, TITLE, SEARCH_TEXT, ASSIGNED_CUSTOMERS, CONFIGURATION};
        try (CSVParser csvParser = new CSVParser(Files.newBufferedReader(dashboardsDump), CSV_DUMP_FORMAT.withHeader(columns))) {
            csvParser.forEach(record -> {
                String customerIdString = record.get(CUSTOMER_ID);
                String assignedCustomersString = record.get(ASSIGNED_CUSTOMERS);
                DashboardId dashboardId = new DashboardId(toUUID(record.get(ID), sql));
                List<CustomerId> customerIds = new ArrayList<>();
                if (!StringUtils.isEmpty(assignedCustomersString)) {
                    try {
                        JsonNode assignedCustomersJson = objectMapper.readTree(assignedCustomersString);
                        Map<String,String> assignedCustomers = objectMapper.treeToValue(assignedCustomersJson, HashMap.class);
                        assignedCustomers.forEach((strCustomerId, title) -> {
                            customerIds.add(new CustomerId(UUID.fromString(strCustomerId)));
                        });
                    } catch (IOException e) {
                        log.error("Unable to parse assigned customers field", e);
                    }
                }
                if (!StringUtils.isEmpty(customerIdString)) {
                    customerIds.add(new CustomerId(toUUID(customerIdString, sql)));
                }
                for (CustomerId customerId : customerIds) {
                    dashboardService.assignDashboardToCustomer(dashboardId, customerId);
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
