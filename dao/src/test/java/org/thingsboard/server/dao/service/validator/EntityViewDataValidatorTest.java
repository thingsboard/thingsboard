// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.service.validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityView;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityViewId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.customer.CustomerDao;
import org.thingsboard.server.dao.entity.EntityService;
import org.thingsboard.server.dao.entityview.EntityViewDao;
import org.thingsboard.server.dao.tenant.TenantService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = EntityViewDataValidator.class)
class EntityViewDataValidatorTest {

    @MockitoBean
    EntityViewDao entityViewDao;
    @MockitoBean
    TenantService tenantService;
    @MockitoBean
    CustomerDao customerDao;
    @MockitoBean
    EntityService entityService;
    @MockitoSpyBean
    EntityViewDataValidator validator;
    TenantId tenantId = TenantId.fromUUID(UUID.fromString("9ef79cdf-37a8-4119-b682-2e7ed4e018da"));
    DeviceId deviceId = new DeviceId(UUID.fromString("3c6f2b7e-5a41-4d8e-9f0b-7e2d1c4a8b95"));

    @BeforeEach
    void setUp() {
        willReturn(true).given(tenantService).tenantExists(tenantId);
    }

    @Test
    void testValidateNameInvocation() {
        EntityView entityView = new EntityView();
        entityView.setName("view");
        entityView.setType("default");
        entityView.setTenantId(tenantId);
        entityView.setEntityId(deviceId);
        willReturn(Optional.of(newDevice(tenantId))).given(entityService).fetchEntity(tenantId, deviceId);

        validator.validateDataImpl(tenantId, entityView);
        verify(validator).validateString("Entity view name", entityView.getName());
        verify(validator).validateString("Entity view type", entityView.getType());
    }

    @Test
    void testValidateEntityViewWithEntityOfSameTenant() {
        EntityView entityView = newEntityView(deviceId);
        willReturn(Optional.of(newDevice(tenantId))).given(entityService).fetchEntity(tenantId, deviceId);

        assertThatCode(() -> validator.validateDataImpl(tenantId, entityView)).doesNotThrowAnyException();
    }

    @Test
    void testValidateEntityViewWithNonExistentEntity() {
        EntityView entityView = newEntityView(deviceId);
        willReturn(Optional.empty()).given(entityService).fetchEntity(tenantId, deviceId);

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, entityView))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Entity view is referencing to non-existent entity!");
    }

    @Test
    void testValidateEntityViewWithEntityOfOtherTenant() {
        EntityView entityView = newEntityView(deviceId);
        TenantId otherTenantId = TenantId.fromUUID(UUID.fromString("b1d4e8a2-6c3f-4a7b-8e5d-2f9c0a1b3d47"));
        willReturn(Optional.of(newDevice(otherTenantId))).given(entityService).fetchEntity(tenantId, deviceId);

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, entityView))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't assign entity view to entity from different tenant!");
    }

    @Test
    void testValidateEntityViewWithEntityOfUnsupportedType() {
        EntityView entityView = newEntityView(deviceId);
        willThrow(new IllegalArgumentException("Unsupported entity type DEVICE")).given(entityService).fetchEntity(tenantId, deviceId);

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, entityView))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Entity view is referencing to non-existent entity!");
    }

    @Test
    void testValidateEntityViewWithEntityWithoutTenant() {
        EntityView entityView = newEntityView(deviceId);
        HasId<DeviceId> entity = () -> deviceId;
        willReturn(Optional.of(entity)).given(entityService).fetchEntity(tenantId, deviceId);

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, entityView))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Can't assign entity view to entity from different tenant!");
    }

    @Test
    void testValidateEntityViewWithoutEntity() {
        EntityView entityView = newEntityView(null);

        assertThatThrownBy(() -> validator.validateDataImpl(tenantId, entityView))
                .isInstanceOf(DataValidationException.class)
                .hasMessage("Entity view should be assigned to entity!");
    }

    @Test
    void testValidateUpdateWithSameNameReturnsViewFoundByName() {
        EntityViewId entityViewId = new EntityViewId(UUID.fromString("5d2a7c1e-8b4f-4e6a-9c3d-1f0e2b7a6c58"));
        EntityView entityView = newEntityView(deviceId);
        entityView.setId(entityViewId);
        EntityView stored = newEntityView(deviceId);
        stored.setId(entityViewId);
        willReturn(Optional.of(stored)).given(entityViewDao).findEntityViewByTenantIdAndName(tenantId.getId(), "view");

        assertThat(validator.validateUpdate(tenantId, entityView)).isSameAs(stored);
        verify(entityViewDao, never()).findById(any(), any());
    }

    private EntityView newEntityView(DeviceId entityId) {
        EntityView entityView = new EntityView();
        entityView.setName("view");
        entityView.setType("default");
        entityView.setTenantId(tenantId);
        entityView.setEntityId(entityId);
        return entityView;
    }

    private Device newDevice(TenantId deviceTenantId) {
        Device device = new Device(deviceId);
        device.setTenantId(deviceTenantId);
        return device;
    }

}
