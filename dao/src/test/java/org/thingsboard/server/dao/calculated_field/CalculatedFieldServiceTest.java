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
package org.thingsboard.server.dao.calculated_field;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.common.util.ThingsBoardExecutors;
import org.thingsboard.server.common.data.calculated_field.CalculatedField;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.dao.service.AbstractServiceTest;
import org.thingsboard.server.dao.service.DaoSqlTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DaoSqlTest
public class CalculatedFieldServiceTest extends AbstractServiceTest {

    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("71c73816-361e-4e57-82ab-e1deaa8b7d66"));

    @Autowired
    private CalculatedFieldService calculatedFieldService;

    private ListeningExecutorService executor;

    @Before
    public void before() {
        executor = MoreExecutors.listeningDecorator(ThingsBoardExecutors.newWorkStealingPool(8, getClass()));
    }

    @After
    public void after() {
        executor.shutdownNow();
    }

    @Test
    public void testSaveCalculatedField() {
        CalculatedField calculatedField = getCalculatedField();
        CalculatedField savedCalculatedField = calculatedFieldService.save(calculatedField);

        assertThat(savedCalculatedField).isNotNull();
        assertThat(savedCalculatedField.getId()).isNotNull();
        assertThat(savedCalculatedField.getCreatedTime()).isGreaterThan(0);
        assertThat(savedCalculatedField.getTenantId()).isEqualTo(calculatedField.getTenantId());
        assertThat(savedCalculatedField.getEntityId()).isEqualTo(calculatedField.getEntityId());
        assertThat(savedCalculatedField.getType()).isEqualTo(calculatedField.getType());
        assertThat(savedCalculatedField.getName()).isEqualTo(calculatedField.getName());
        assertThat(savedCalculatedField.getConfiguration()).isEqualTo(calculatedField.getConfiguration());
        assertThat(savedCalculatedField.getVersion()).isEqualTo(calculatedField.getVersion());

        savedCalculatedField.setName("Test CF");

        CalculatedField updatedCalculatedField = calculatedFieldService.save(savedCalculatedField);

        assertThat(updatedCalculatedField).isEqualTo(savedCalculatedField);

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

    @Test
    public void testFindCalculatedFieldById() {
        CalculatedField calculatedField = getCalculatedField();
        CalculatedField savedCalculatedField = calculatedFieldService.save(calculatedField);

        CalculatedField fetchedCalculatedField = calculatedFieldService.findById(tenantId, savedCalculatedField.getId());

        assertThat(fetchedCalculatedField).isEqualTo(savedCalculatedField);

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());
    }

    @Test
    public void testDeleteCalculatedField() {
        CalculatedField calculatedField = getCalculatedField();
        CalculatedField savedCalculatedField = calculatedFieldService.save(calculatedField);

        calculatedFieldService.deleteCalculatedField(tenantId, savedCalculatedField.getId());

        assertThat(calculatedFieldService.findById(tenantId, savedCalculatedField.getId())).isNull();
    }

    private CalculatedField getCalculatedField() {
        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(tenantId);
        calculatedField.setEntityId(DEVICE_ID);
        calculatedField.setType("Simple");
        calculatedField.setName("Test Calculated Field");
        calculatedField.setConfigurationVersion(1);
        calculatedField.setConfiguration(JacksonUtil.toJsonNode("{\n" +
                "    \"T\": {\n" +
                "    \"key\": \"temperature\",\n" +
                "    \"type\": \"TIME_SERIES\"\n" +
                "    },\n" +
                "    \"H\": {\n" +
                "    \"key\": \"humidity\",\n" +
                "    \"type\": \"TIME_SERIES\",\n" +
                "    \"defaultValue\": 50\n" +
                "    }\n" +
                " }\n"));
        calculatedField.setVersion(1L);
        return calculatedField;
    }

}
