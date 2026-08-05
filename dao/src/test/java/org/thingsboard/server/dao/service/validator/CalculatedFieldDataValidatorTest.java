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
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.ComputeOn;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesImmediateOutputStrategy;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.relation.EntityRelation;
import org.thingsboard.server.common.data.relation.RelationTypeGroup;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.relation.RelationService;
import org.thingsboard.server.dao.usagerecord.DefaultApiLimitService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@SpringBootTest(classes = CalculatedFieldDataValidator.class)
public class CalculatedFieldDataValidatorTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("7b5229e9-166e-41a9-a257-3b1dafad1b04"));
    private final CalculatedFieldId CALCULATED_FIELD_ID = new CalculatedFieldId(UUID.fromString("060fbe45-fbb2-4549-abf3-f72a6be3cb9f"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("9dcb1c1a-7b1a-4b1a-9c0e-1d3a5c6c7b8a"));
    private final AssetId ASSET_ID = new AssetId(UUID.fromString("5f6f8f0b-2a3c-4d5e-8f90-1a2b3c4d5e6f"));
    private final EdgeId EDGE_ID = new EdgeId(UUID.fromString("3a2b1c0d-4e5f-6a7b-8c9d-0e1f2a3b4c5d"));

    @MockitoBean
    private CalculatedFieldDao calculatedFieldDao;
    @MockitoBean
    private DefaultApiLimitService apiLimitService;
    @MockitoBean
    private RelationService relationService;
    @MockitoBean
    private EdgeSynchronizationManager edgeSynchronizationManager;
    @MockitoSpyBean
    private CalculatedFieldDataValidator validator;

    @BeforeEach
    public void setUp() {
        given(edgeSynchronizationManager.getEdgeId()).willReturn(new ThreadLocal<>());
    }

    @Test
    public void testUpdateNonExistingCalculatedField() {
        CalculatedField calculatedField = new CalculatedField(CALCULATED_FIELD_ID);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test");

        given(calculatedFieldDao.findById(TENANT_ID, CALCULATED_FIELD_ID.getId())).willReturn(null);

        assertThatThrownBy(() -> validator.validateUpdate(TENANT_ID, calculatedField))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't update non existing calculated field!");
    }

    @Test
    public void testEdgeOnlyRejectedWhenDeviceIsNotAssignedToEdge() {
        givenNotAssignedToEdge(DEVICE_ID);

        assertThatThrownBy(() -> validator.validateDataImpl(TENANT_ID, edgeOnlyCf(DEVICE_ID)))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Calculated field computed on the edge requires device to be assigned to an edge!");
    }

    @Test
    public void testEdgeOnlyRejectedWhenAssetIsNotAssignedToEdge() {
        givenNotAssignedToEdge(ASSET_ID);

        assertThatThrownBy(() -> validator.validateDataImpl(TENANT_ID, edgeOnlyCf(ASSET_ID)))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Calculated field computed on the edge requires asset to be assigned to an edge!");
    }

    @Test
    public void testEdgeOnlyAllowedWhenDeviceIsAssignedToEdge() {
        givenAssignedToEdge(DEVICE_ID);

        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, edgeOnlyCf(DEVICE_ID))).doesNotThrowAnyException();
    }

    @Test
    public void testEdgeOnlyAllowedForProfileBoundCalculatedField() {
        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, edgeOnlyCf(new DeviceProfileId(UUID.randomUUID())))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, edgeOnlyCf(new AssetProfileId(UUID.randomUUID())))).doesNotThrowAnyException();
    }

    @Test
    public void testEdgeOnlyAllowedDuringEdgeSynchronization() {
        givenNotAssignedToEdge(DEVICE_ID);
        ThreadLocal<EdgeId> edgeIdHolder = new ThreadLocal<>();
        edgeIdHolder.set(EDGE_ID);
        given(edgeSynchronizationManager.getEdgeId()).willReturn(edgeIdHolder);

        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, edgeOnlyCf(DEVICE_ID))).doesNotThrowAnyException();
    }

    @Test
    public void testEdgeAllowedWithImmediateOutputStrategy() {
        givenAssignedToEdge(DEVICE_ID);
        CalculatedField calculatedField = edgeOnlyCf(DEVICE_ID);
        ((TimeSeriesOutput) calculatedField.getConfiguration().getOutput()).setStrategy(new TimeSeriesImmediateOutputStrategy());

        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, calculatedField)).doesNotThrowAnyException();
    }

    @Test
    public void testComputeOnCloudNeverRequiresAnEdge() {
        CalculatedField defaulted = cf(DEVICE_ID, null);
        CalculatedField cloud = cf(DEVICE_ID, ComputeOn.CLOUD);
        ((TimeSeriesOutput) cloud.getConfiguration().getOutput()).setStrategy(new TimeSeriesImmediateOutputStrategy());

        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, defaulted)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, cloud)).doesNotThrowAnyException();
    }

    private void givenNotAssignedToEdge(EntityId entityId) {
        given(edgeSynchronizationManager.getEdgeId()).willReturn(new ThreadLocal<>());
        given(relationService.findByToAndType(TENANT_ID, entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .willReturn(List.of());
    }

    private void givenAssignedToEdge(EntityId entityId) {
        given(edgeSynchronizationManager.getEdgeId()).willReturn(new ThreadLocal<>());
        given(relationService.findByToAndType(TENANT_ID, entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE))
                .willReturn(List.of(new EntityRelation(EDGE_ID, entityId, EntityRelation.CONTAINS_TYPE, RelationTypeGroup.EDGE)));
    }

    private CalculatedField edgeOnlyCf(EntityId entityId) {
        return cf(entityId, ComputeOn.EDGE);
    }

    private CalculatedField cf(EntityId entityId, ComputeOn computeOn) {
        CalculatedField calculatedField = new CalculatedField(CALCULATED_FIELD_ID);
        calculatedField.setTenantId(TENANT_ID);
        calculatedField.setEntityId(entityId);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test");
        calculatedField.setComputeOn(computeOn);

        Argument argument = new Argument();
        argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));

        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("result");

        SimpleCalculatedFieldConfiguration configuration = new SimpleCalculatedFieldConfiguration();
        configuration.setArguments(Map.of("temperature", argument));
        configuration.setExpression("temperature * 2");
        configuration.setOutput(output);
        calculatedField.setConfiguration(configuration);

        return calculatedField;
    }

}
