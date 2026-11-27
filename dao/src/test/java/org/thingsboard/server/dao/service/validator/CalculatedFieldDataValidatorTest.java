// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.HasTenantId;
import org.thingsboard.server.common.data.cf.CalculatedField;
import org.thingsboard.server.common.data.cf.CalculatedFieldType;
import org.thingsboard.server.common.data.cf.ComputeOn;
import org.thingsboard.server.common.data.cf.configuration.Argument;
import org.thingsboard.server.common.data.cf.configuration.ArgumentType;
import org.thingsboard.server.common.data.cf.configuration.ReferencedEntityKey;
import org.thingsboard.server.common.data.cf.configuration.SimpleCalculatedFieldConfiguration;
import org.thingsboard.server.common.data.cf.configuration.TimeSeriesOutput;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EdgeId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.dao.Dao;
import org.thingsboard.server.dao.cf.CalculatedFieldDao;
import org.thingsboard.server.dao.device.DeviceDao;
import org.thingsboard.server.dao.edge.EdgeService;
import org.thingsboard.server.dao.edge.EdgeSynchronizationManager;
import org.thingsboard.server.dao.entity.EntityDaoRegistry;
import org.thingsboard.server.dao.usagerecord.DefaultApiLimitService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = CalculatedFieldDataValidator.class)
public class CalculatedFieldDataValidatorTest {

    private final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("7b5229e9-166e-41a9-a257-3b1dafad1b04"));
    private final TenantId OTHER_TENANT_ID = TenantId.fromUUID(UUID.fromString("2f3d5c1e-7a52-4c8b-9d36-0b0e4f1a6c27"));
    private final CalculatedFieldId CALCULATED_FIELD_ID = new CalculatedFieldId(UUID.fromString("060fbe45-fbb2-4549-abf3-f72a6be3cb9f"));
    private final DeviceId DEVICE_ID = new DeviceId(UUID.fromString("5a1f0e8c-3b44-4f0e-8e2a-91c7d6b3e512"));
    private final AssetId ASSET_ID = new AssetId(UUID.fromString("5f6f8f0b-2a3c-4d5e-8f90-1a2b3c4d5e6f"));
    private final EdgeId EDGE_ID = new EdgeId(UUID.fromString("3a2b1c0d-4e5f-6a7b-8c9d-0e1f2a3b4c5d"));
    private final DeviceId REFERENCED_DEVICE_ID = new DeviceId(UUID.fromString("c8e2b7d4-1f6a-4e93-a5c0-7d2f9b4e8a31"));

    @MockitoBean
    private CalculatedFieldDao calculatedFieldDao;
    @MockitoBean
    private DefaultApiLimitService apiLimitService;
    @MockitoBean
    private EntityDaoRegistry entityDaoRegistry;
    @MockitoBean
    private EdgeService edgeService;
    @MockitoBean
    private EdgeSynchronizationManager edgeSynchronizationManager;
    @MockitoSpyBean
    private CalculatedFieldDataValidator validator;

    private final DeviceDao deviceDao = mock(DeviceDao.class);

    @BeforeEach
    public void setUp() {
        given(entityDaoRegistry.<Device>getDao(EntityType.DEVICE)).willReturn(deviceDao);
        given(deviceDao.findById(TENANT_ID, DEVICE_ID.getId())).willReturn(getDevice(DEVICE_ID, TENANT_ID));
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
    public void testUpdateCalculatedFieldOfOtherTenant() {
        CalculatedField calculatedField = getCalculatedField(TENANT_ID);
        calculatedField.setId(CALCULATED_FIELD_ID);
        CalculatedField stored = getCalculatedField(TENANT_ID);
        stored.setId(CALCULATED_FIELD_ID);
        stored.setTenantId(OTHER_TENANT_ID);

        given(calculatedFieldDao.findById(TENANT_ID, CALCULATED_FIELD_ID.getId())).willReturn(stored);

        assertThatThrownBy(() -> validator.validateUpdate(TENANT_ID, calculatedField))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't update calculated field tenant id!");
    }

    @Test
    public void testValidateCalculatedFieldWithOwnReferences() {
        given(deviceDao.findById(TENANT_ID, REFERENCED_DEVICE_ID.getId())).willReturn(getDevice(REFERENCED_DEVICE_ID, TENANT_ID));

        assertThatCode(() -> validator.validate(getCalculatedField(TENANT_ID, REFERENCED_DEVICE_ID), CalculatedField::getTenantId))
                .doesNotThrowAnyException();
    }

    @Test
    public void testValidateCalculatedFieldWithOtherTenantReference() {
        assertThatThrownBy(() -> validator.validate(getCalculatedField(OTHER_TENANT_ID), CalculatedField::getTenantId))
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    public void testValidateCalculatedFieldWithOtherTenantDeviceReference() {
        given(deviceDao.findById(TENANT_ID, REFERENCED_DEVICE_ID.getId())).willReturn(getDevice(REFERENCED_DEVICE_ID, OTHER_TENANT_ID));

        assertThatThrownBy(() -> validator.validate(getCalculatedField(REFERENCED_DEVICE_ID), CalculatedField::getTenantId))
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    public void testValidateCalculatedFieldWithMissingDeviceReference() {
        assertThatThrownBy(() -> validator.validate(getCalculatedField(REFERENCED_DEVICE_ID), CalculatedField::getTenantId))
                .isInstanceOf(DataValidationException.class);
    }

    @Test
    public void testValidateCalculatedFieldWithOtherTenantTarget() {
        given(deviceDao.findById(TENANT_ID, DEVICE_ID.getId())).willReturn(getDevice(DEVICE_ID, OTHER_TENANT_ID));

        assertThatThrownBy(() -> validator.validate(getCalculatedField(TENANT_ID), CalculatedField::getTenantId))
                .isInstanceOf(DataValidationException.class);
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
    public void testComputeOnCloudNeverRequiresAnEdge() {
        CalculatedField defaulted = cf(DEVICE_ID, null);
        CalculatedField cloud = cf(DEVICE_ID, ComputeOn.CLOUD);

        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, defaulted)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateDataImpl(TENANT_ID, cloud)).doesNotThrowAnyException();
    }

    private CalculatedField getCalculatedField(EntityId... refEntityIds) {
        SimpleCalculatedFieldConfiguration configuration = new SimpleCalculatedFieldConfiguration();
        Map<String, Argument> arguments = new LinkedHashMap<>();
        for (int i = 0; i < refEntityIds.length; i++) {
            Argument argument = new Argument();
            argument.setRefEntityId(refEntityIds[i]);
            argument.setRefEntityKey(new ReferencedEntityKey("temperature", ArgumentType.TS_LATEST, null));
            arguments.put("a" + i, argument);
        }
        configuration.setArguments(arguments);
        configuration.setExpression("a0");
        TimeSeriesOutput output = new TimeSeriesOutput();
        output.setName("output");
        configuration.setOutput(output);

        CalculatedField calculatedField = new CalculatedField();
        calculatedField.setTenantId(TENANT_ID);
        calculatedField.setEntityId(DEVICE_ID);
        calculatedField.setType(CalculatedFieldType.SIMPLE);
        calculatedField.setName("Test");
        calculatedField.setConfiguration(configuration);
        return calculatedField;
    }

    private Device getDevice(DeviceId deviceId, TenantId tenantId) {
        Device device = new Device(deviceId);
        device.setTenantId(tenantId);
        return device;
    }

    private void givenNotAssignedToEdge(EntityId entityId) {
        given(edgeService.findRelatedEdgeIdsByEntityId(eq(TENANT_ID), eq(entityId), any(PageLink.class)))
                .willReturn(PageData.emptyPageData());
    }

    private void givenAssignedToEdge(EntityId entityId) {
        given(edgeService.findRelatedEdgeIdsByEntityId(eq(TENANT_ID), eq(entityId), any(PageLink.class)))
                .willReturn(new PageData<>(List.of(EDGE_ID), 1, 1, false));
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

        if (!DEVICE_ID.equals(entityId)) {
            givenBelongsToTenant(entityId);
        }
        return calculatedField;
    }

    @SuppressWarnings("unchecked")
    private void givenBelongsToTenant(EntityId entityId) {
        Dao<HasTenantId> dao = mock(Dao.class);
        given(entityDaoRegistry.<HasTenantId>getDao(entityId.getEntityType())).willReturn(dao);
        HasTenantId entity = mock(HasTenantId.class);
        given(entity.getTenantId()).willReturn(TENANT_ID);
        given(dao.findById(TENANT_ID, entityId.getId())).willReturn(entity);
    }

}
