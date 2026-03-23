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
package org.thingsboard.server.controller;

import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.server.common.data.query.ComplexOperation;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityCountQuery;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DaoSqlTest
@DirtiesContext
@TestPropertySource(properties = {
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api.supported=false",
        "sql.query.key-filters-or-conditions.enabled=false"
})
public class OrConditionsDisabledEntityQueryControllerTest extends EntityQueryControllerTest {

    @Test
    public void testOrKeyFiltersOperationRejectedWhenDisabled() throws Exception {
        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        // POST a query with OR operation -- should be rejected with 400
        EntityCountQuery orQuery = new EntityCountQuery(filter, Collections.emptyList(), ComplexOperation.OR);
        String errorMessage = getErrorMessage(
                doPost("/api/entitiesQuery/count", orQuery).andExpect(status().isBadRequest())
        );
        assertThat(errorMessage).contains("OR conditions between key filters are disabled");

        // POST a query without keyFiltersOperation (null/AND) -- should still succeed
        EntityCountQuery andQuery = new EntityCountQuery(filter, Collections.emptyList());
        doPost("/api/entitiesQuery/count", andQuery).andExpect(status().isOk());

        // POST a query with explicit AND -- should also succeed
        EntityCountQuery explicitAndQuery = new EntityCountQuery(filter, Collections.emptyList(), ComplexOperation.AND);
        doPost("/api/entitiesQuery/count", explicitAndQuery).andExpect(status().isOk());
    }

}
