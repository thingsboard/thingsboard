// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.device;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.security.DeviceCredentials;
import org.thingsboard.server.dao.entity.EntityCountService;
import org.thingsboard.server.dao.entityview.EntityViewService;
import org.thingsboard.server.dao.event.EventService;
import org.thingsboard.server.dao.service.validator.DeviceDataValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.tenant.TenantService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeviceServiceImplCredentialsEvictionTest {

    private final TenantId tenantId = TenantId.fromUUID(UUID.randomUUID());
    private final DeviceId deviceId = new DeviceId(UUID.randomUUID());
    private DeviceCredentialsService credentialsService;
    private DeviceCredentialsEvictionHook hook;
    private DeviceServiceImpl service;

    @BeforeEach
    void setUp() {
        credentialsService = mock(DeviceCredentialsService.class);
        hook = mock(DeviceCredentialsEvictionHook.class);
        service = new DeviceServiceImpl(mock(DeviceDao.class), mock(DeviceInfoDao.class), credentialsService, mock(DeviceProfileService.class),
                mock(EntityViewService.class), mock(EventService.class), mock(TenantService.class), mock(DeviceDataValidator.class),
                mock(EntityCountService.class), mock(JpaExecutorService.class));
        ReflectionTestUtils.setField(service, "credentialsEvictionHooks", List.of(hook));
        DeviceCredentials credentials = new DeviceCredentials();
        credentials.setDeviceId(deviceId);
        credentials.setCredentialsId("token");
        when(credentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId)).thenReturn(credentials);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void givenNoTransaction_whenEvict_thenHookDeletesEntryImmediately() {
        service.evictCredentialsFromValidationCaches(tenantId, deviceId);

        ArgumentCaptor<DeviceCredentialsEvictEvent> captor = ArgumentCaptor.forClass(DeviceCredentialsEvictEvent.class);
        verify(hook).onDeviceCredentialsEvict(captor.capture());
        assertThat(captor.getValue().getNewCredentialsId()).isEqualTo("token");
        assertThat(captor.getValue().getOldCredentialsId()).isNull();
        assertThat(captor.getValue().isDeleted()).isFalse();
    }

    @Test
    void givenActiveTransaction_whenEvict_thenHookRunsOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();

        service.evictCredentialsFromValidationCaches(tenantId, deviceId);
        verify(hook, never()).onDeviceCredentialsEvict(any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(hook).onDeviceCredentialsEvict(any());
    }

    @Test
    void givenCredentialsLookupFails_whenEvict_thenNoException() {
        when(credentialsService.findDeviceCredentialsByDeviceId(tenantId, deviceId)).thenThrow(new RuntimeException("db down"));

        assertThatCode(() -> service.evictCredentialsFromValidationCaches(tenantId, deviceId)).doesNotThrowAnyException();
        verify(hook, never()).onDeviceCredentialsEvict(any());
    }

}
