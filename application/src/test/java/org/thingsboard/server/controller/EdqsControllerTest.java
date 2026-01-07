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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.AttributeScope;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.edqs.EdqsSyncRequest;
import org.thingsboard.server.common.data.edqs.ToCoreEdqsRequest;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.kv.AttributeKvEntry;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.query.DeviceTypeFilter;
import org.thingsboard.server.common.data.query.EntityData;
import org.thingsboard.server.common.data.query.EntityDataPageLink;
import org.thingsboard.server.common.data.query.EntityDataQuery;
import org.thingsboard.server.common.data.query.EntityKey;
import org.thingsboard.server.common.data.query.EntityKeyType;
import org.thingsboard.server.common.data.query.EntityKeyValueType;
import org.thingsboard.server.common.data.query.FilterPredicateValue;
import org.thingsboard.server.common.data.query.KeyFilter;
import org.thingsboard.server.common.data.query.StringFilterPredicate;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

@DaoSqlTest
@TestPropertySource(properties = {
        "queue.edqs.sync.enabled=true",
        "queue.edqs.api.supported=true",
        "queue.edqs.api.auto_enable=true",
        "queue.edqs.mode=local"
})
public class EdqsControllerTest extends AbstractControllerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Before
    public void beforeEdqsControllerTest() throws Exception {
        loginTenantAdmin();
    }

    @Test
    public void testEdqsSync() throws Exception {
        List<Device> devices = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Device device = new Device();
            device.setName("Device" + i);
            device.setType("default");
            device.setLabel("testLabel" + (int) (Math.random() * 1000));
            ObjectNode additionalInfo = JacksonUtil.newObjectNode();
            additionalInfo.put("gateway", true);
            device.setAdditionalInfo(additionalInfo);
            devices.add(doPost("/api/device", device, Device.class));
            Thread.sleep(1);
        }

        DeviceTypeFilter filter = new DeviceTypeFilter();
        filter.setDeviceTypes(List.of("default"));
        filter.setDeviceNameFilter("");

        List<EntityKey> entityFields = Collections.singletonList(new EntityKey(EntityKeyType.ENTITY_FIELD, "name"));

        EntityDataPageLink pageLink = new EntityDataPageLink(10, 0, null, null);
        EntityDataQuery query = new EntityDataQuery(filter, pageLink, entityFields, null, Collections.singletonList(getGatewayFilter()));
        await().atMost(TIMEOUT, TimeUnit.SECONDS)
                .until(() -> doPostWithTypedResponse("/api/entitiesQuery/find", query, new TypeReference<PageData<EntityData>>() {
                }), result -> result.getTotalElements() == 3);

        // update db
        Device device1 = devices.get(0);
        device1.setAdditionalInfo(JacksonUtil.newObjectNode());
        jdbcTemplate.execute("update device set additional_info = '{}' where id = '" + device1.getId().getId().toString() + "'");

        // do edqs sync
        loginSysAdmin();
        ToCoreEdqsRequest syncRequest = new ToCoreEdqsRequest(new EdqsSyncRequest(), null);
        doPost("/api/edqs/system/request", syncRequest);

        //check sync is finished
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> {
            Optional<AttributeKvEntry> attribute = attributesService.find(TenantId.SYS_TENANT_ID, TenantId.SYS_TENANT_ID, AttributeScope.SERVER_SCOPE, "edqsSyncState").get();
            return attribute.isPresent() && attribute.get().getJsonValue().isPresent() &&
                    attribute.get().getJsonValue().get().contains("\"status\":\"FINISHED\"");
        });

        // check if the count is updated
        loginTenantAdmin();
        await().atMost(TIMEOUT, TimeUnit.SECONDS).until(() -> doPostWithTypedResponse("/api/entitiesQuery/find", query, new TypeReference<PageData<EntityData>>() {
        }), result -> result.getTotalElements() == 2);
    }

    private KeyFilter getGatewayFilter() {
        KeyFilter additionalInfoFilter = new KeyFilter();
        additionalInfoFilter.setKey(new EntityKey(EntityKeyType.ENTITY_FIELD, "additionalInfo"));
        additionalInfoFilter.setValueType(EntityKeyValueType.STRING);
        StringFilterPredicate predicate = new StringFilterPredicate();
        predicate.setValue(FilterPredicateValue.fromString("\"gateway\":true"));
        predicate.setOperation(StringFilterPredicate.StringOperation.CONTAINS);
        additionalInfoFilter.setPredicate(predicate);
        return additionalInfoFilter;
    }
}
