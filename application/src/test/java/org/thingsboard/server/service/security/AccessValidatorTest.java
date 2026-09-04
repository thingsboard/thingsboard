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
package org.thingsboard.server.service.security;

import com.google.common.util.concurrent.FutureCallback;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thingsboard.server.common.data.ApiUsageState;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.OtaPackageInfo;
import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.asset.AssetProfile;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.exception.ThingsboardException;
import org.thingsboard.server.common.data.id.ApiUsageStateId;
import org.thingsboard.server.common.data.id.AssetProfileId;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.EntityIdFactory;
import org.thingsboard.server.common.data.id.OtaPackageId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.common.data.security.Authority;
import org.thingsboard.server.dao.asset.AssetProfileService;
import org.thingsboard.server.dao.device.DeviceProfileService;
import org.thingsboard.server.dao.exception.IncorrectParameterException;
import org.thingsboard.server.dao.ota.OtaPackageService;
import org.thingsboard.server.dao.usagerecord.ApiUsageStateService;
import org.thingsboard.server.service.security.model.SecurityUser;
import org.thingsboard.server.service.security.permission.AccessControlService;
import org.thingsboard.server.service.security.permission.Operation;
import org.thingsboard.server.service.security.permission.Resource;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
public class AccessValidatorTest {

    private static final TenantId TENANT_ID = TenantId.fromUUID(UUID.fromString("1a6e3c46-4b6b-4a45-9a06-05a3b1b1e4b6"));

    private static final Set<EntityType> EXPECTED_ENTITY_TYPES_WITH_TS_AND_ATTRIBUTES = EnumSet.of(
            EntityType.DEVICE, EntityType.ASSET, EntityType.ENTITY_VIEW, EntityType.CUSTOMER, EntityType.TENANT,
            EntityType.TENANT_PROFILE, EntityType.USER, EntityType.EDGE, EntityType.RULE_CHAIN, EntityType.API_USAGE_STATE);

    private static final List<Operation> EXPECTED_TS_AND_ATTRIBUTES_OPERATIONS = List.of(
            Operation.READ_TELEMETRY, Operation.WRITE_TELEMETRY, Operation.READ_ATTRIBUTES, Operation.WRITE_ATTRIBUTES);

    @Mock
    private DeviceProfileService deviceProfileService;
    @Mock
    private AssetProfileService assetProfileService;
    @Mock
    private ApiUsageStateService apiUsageStateService;
    @Mock
    private OtaPackageService otaPackageService;
    @Mock
    private AccessControlService accessControlService;

    @InjectMocks
    private AccessValidator accessValidator;

    private SecurityUser customerUser;
    private RecordingCallback callback;

    @BeforeEach
    void setUp() {
        User user = new User(new UserId(UUID.randomUUID()));
        user.setAuthority(Authority.CUSTOMER_USER);
        user.setTenantId(TENANT_ID);
        user.setCustomerId(new CustomerId(UUID.randomUUID()));
        customerUser = new SecurityUser(user, true, null);
        callback = new RecordingCallback();
    }

    @ParameterizedTest
    @MethodSource("entitiesWithSynchronousValidation")
    public void givenPermissionDenied_whenValidate_thenAccessDeniedAndNoOkResult(EntityId entityId, Resource resource) throws ThingsboardException {
        stubEntityLookup(entityId);
        denyPermission(resource);

        accessValidator.validate(customerUser, Operation.WRITE, entityId, callback);

        assertSingleResult(ValidationResultCode.ACCESS_DENIED);
    }

    @ParameterizedTest
    @MethodSource("entitiesWithSynchronousValidation")
    public void givenEntityNotFound_whenValidate_thenEntityNotFoundAndNoOkResult(EntityId entityId) {
        accessValidator.validate(customerUser, Operation.WRITE, entityId, callback);

        assertSingleResult(ValidationResultCode.ENTITY_NOT_FOUND);
        verifyNoInteractions(accessControlService);
    }

    private static Stream<Arguments> entitiesWithSynchronousValidation() {
        return Stream.of(
                Arguments.of(new DeviceProfileId(UUID.randomUUID()), Resource.DEVICE_PROFILE),
                Arguments.of(new AssetProfileId(UUID.randomUUID()), Resource.ASSET_PROFILE),
                Arguments.of(new OtaPackageId(UUID.randomUUID()), Resource.OTA_PACKAGE)
        );
    }

    @Test
    public void givenScopeResolvedFromResources_whenRead_thenMatchesExpectedEntityTypesAndOperations() {
        assertThat(Resource.ENTITY_TYPES_WITH_TS_AND_ATTRIBUTES)
                .containsExactlyInAnyOrderElementsOf(EXPECTED_ENTITY_TYPES_WITH_TS_AND_ATTRIBUTES);
        assertThat(Operation.TS_AND_ATTRIBUTES_OPERATIONS)
                .containsExactlyInAnyOrderElementsOf(EXPECTED_TS_AND_ATTRIBUTES_OPERATIONS);
    }

    @ParameterizedTest
    @MethodSource("tsAndAttributesOperationsOnEntityTypesOutOfScope")
    public void givenEntityTypeWithoutTsAndAttributes_whenValidate_thenFailsAndEntityIsNotFetched(EntityId entityId, Operation operation) {
        accessValidator.validate(customerUser, operation, entityId, callback);

        assertSingleFailure(IncorrectParameterException.class);
        verifyNoInteractions(deviceProfileService, assetProfileService, apiUsageStateService, otaPackageService, accessControlService);
    }

    private static Stream<Arguments> tsAndAttributesOperationsOnEntityTypesOutOfScope() {
        return EnumSet.complementOf(EnumSet.copyOf(EXPECTED_ENTITY_TYPES_WITH_TS_AND_ATTRIBUTES)).stream()
                .map(entityType -> EntityIdFactory.getByTypeAndUuid(entityType, UUID.randomUUID()))
                .flatMap(entityId -> EXPECTED_TS_AND_ATTRIBUTES_OPERATIONS.stream().map(operation -> Arguments.of(entityId, operation)));
    }

    @Test
    public void givenPermissionDeniedForApiUsageState_whenValidate_thenAccessDeniedAndNoOkResult() throws ThingsboardException {
        ApiUsageStateId apiUsageStateId = new ApiUsageStateId(UUID.randomUUID());
        given(apiUsageStateService.findApiUsageStateById(TENANT_ID, apiUsageStateId)).willReturn(new ApiUsageState(apiUsageStateId));
        denyPermission(Resource.API_USAGE_STATE);

        accessValidator.validate(customerUser, Operation.READ_TELEMETRY, apiUsageStateId, callback);

        assertSingleResult(ValidationResultCode.ACCESS_DENIED);
    }

    @ParameterizedTest
    @EnumSource(value = Operation.class, names = {"WRITE_TELEMETRY", "WRITE_ATTRIBUTES", "READ_ATTRIBUTES"})
    public void givenOperationOtherThanReadTelemetryOnApiUsageState_whenValidate_thenAccessDeniedAndStateIsNotFetched(Operation operation) {
        ApiUsageStateId apiUsageStateId = new ApiUsageStateId(UUID.randomUUID());

        accessValidator.validate(customerUser, operation, apiUsageStateId, callback);

        assertSingleResult(ValidationResultCode.ACCESS_DENIED);
        verify(apiUsageStateService, never()).findApiUsageStateById(any(), any());
    }

    @Test
    public void givenPermissionGranted_whenValidate_thenOkResultWithEntity() {
        DeviceProfileId deviceProfileId = new DeviceProfileId(UUID.randomUUID());
        DeviceProfile deviceProfile = new DeviceProfile(deviceProfileId);
        given(deviceProfileService.findDeviceProfileById(TENANT_ID, deviceProfileId)).willReturn(deviceProfile);

        accessValidator.validate(customerUser, Operation.READ, deviceProfileId, callback);

        assertSingleResult(ValidationResultCode.OK);
        assertThat(callback.results.get(0).getV()).isSameAs(deviceProfile);
    }

    private void stubEntityLookup(EntityId entityId) {
        switch (entityId.getEntityType()) {
            case DEVICE_PROFILE -> given(deviceProfileService.findDeviceProfileById(TENANT_ID, (DeviceProfileId) entityId))
                    .willReturn(new DeviceProfile((DeviceProfileId) entityId));
            case ASSET_PROFILE -> given(assetProfileService.findAssetProfileById(TENANT_ID, (AssetProfileId) entityId))
                    .willReturn(new AssetProfile((AssetProfileId) entityId));
            case OTA_PACKAGE -> given(otaPackageService.findOtaPackageInfoById(TENANT_ID, (OtaPackageId) entityId))
                    .willReturn(new OtaPackageInfo((OtaPackageId) entityId));
            default -> throw new IllegalArgumentException("Unexpected entity type: " + entityId.getEntityType());
        }
    }

    private void denyPermission(Resource resource) throws ThingsboardException {
        willThrow(new ThingsboardException("You don't have permission to perform this operation!", ThingsboardErrorCode.PERMISSION_DENIED))
                .given(accessControlService).checkPermission(eq(customerUser), eq(resource), any(Operation.class), any(EntityId.class), any());
    }

    private void assertSingleResult(ValidationResultCode expectedCode) {
        assertThat(callback.failures).isEmpty();
        assertThat(callback.results).singleElement()
                .extracting(ValidationResult::getResultCode).isEqualTo(expectedCode);
    }

    private void assertSingleFailure(Class<? extends Throwable> expectedType) {
        assertThat(callback.results).isEmpty();
        assertThat(callback.failures).singleElement().isInstanceOf(expectedType);
    }

    private static class RecordingCallback implements FutureCallback<ValidationResult> {

        private final List<ValidationResult> results = new ArrayList<>();
        private final List<Throwable> failures = new ArrayList<>();

        @Override
        public void onSuccess(ValidationResult result) {
            results.add(result);
        }

        @Override
        public void onFailure(Throwable t) {
            failures.add(t);
        }

    }

}
