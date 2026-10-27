// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.gen.transport.TransportProtos;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ValidateResponseTransformTest {

    private final DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
    private TransportDeviceProfileCache deviceProfileCache;
    private DefaultTransportService service;

    @BeforeEach
    void setUp() {
        deviceProfileCache = mock(TransportDeviceProfileCache.class);
        service = mock(DefaultTransportService.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "deviceProfileCache", deviceProfileCache);
    }

    private TransportProtos.ValidateDeviceCredentialsResponseMsg.Builder msgBuilder() {
        return TransportProtos.ValidateDeviceCredentialsResponseMsg.newBuilder()
                .setCredentialsBody("body")
                .setDeviceInfo(TransportProtos.DeviceInfoProto.newBuilder()
                        .setDeviceIdMSB(1).setDeviceIdLSB(2)
                        .setTenantIdMSB(3).setTenantIdLSB(4)
                        .setCustomerIdMSB(5).setCustomerIdLSB(6)
                        .setDeviceProfileIdMSB(profileId.getId().getMostSignificantBits())
                        .setDeviceProfileIdLSB(profileId.getId().getLeastSignificantBits())
                        .setDeviceName("dev").setDeviceType("default").build());
    }

    private DeviceProfile profile(DeviceTransportType type, Long version) {
        DeviceProfile p = new DeviceProfile(profileId);
        p.setTransportType(type);
        p.setVersion(version);
        return p;
    }

    @Test
    void givenMsgWithProfile_whenTransform_thenGetOrCreateUsed() {
        DeviceProfile profile = profile(DeviceTransportType.DEFAULT, 1L);
        TransportProtos.DeviceProfileProto proto = TransportProtos.DeviceProfileProto.newBuilder().build();
        when(deviceProfileCache.getOrCreate(any(), any())).thenReturn(profile);

        var result = service.toValidateResponse(msgBuilder().setDeviceProfile(proto).build(), DeviceTransportType.DEFAULT);

        assertThat(result.getDeviceProfile()).isSameAs(profile);
        assertThat(result.getCredentials()).isEqualTo("body");
        verify(deviceProfileCache).getOrCreate(profileId, proto);
        verify(deviceProfileCache, never()).get(any());
    }

    @Test
    void givenMsgWithoutProfile_whenTransform_thenProfileFetchedById() {
        DeviceProfile profile = profile(DeviceTransportType.DEFAULT, 5L);
        when(deviceProfileCache.get(profileId)).thenReturn(profile);

        var result = service.toValidateResponse(msgBuilder().setDeviceProfileVersion(5).build(), DeviceTransportType.DEFAULT);

        assertThat(result.getDeviceProfile()).isSameAs(profile);
        verify(deviceProfileCache).get(profileId);
        verify(deviceProfileCache, never()).evict(any());
    }

    @Test
    void givenNewerProfileVersionInMsg_whenTransform_thenEvictAndRefetch() {
        DeviceProfile stale = profile(DeviceTransportType.DEFAULT, 1L);
        DeviceProfile fresh = profile(DeviceTransportType.DEFAULT, 2L);
        when(deviceProfileCache.get(profileId)).thenReturn(stale, fresh);

        var result = service.toValidateResponse(msgBuilder().setDeviceProfileVersion(2).build(), DeviceTransportType.DEFAULT);

        assertThat(result.getDeviceProfile()).isSameAs(fresh);
        InOrder order = inOrder(deviceProfileCache);
        order.verify(deviceProfileCache).get(profileId);
        order.verify(deviceProfileCache).evict(profileId);
        order.verify(deviceProfileCache).get(profileId);
    }

    @Test
    void givenCachedOkButProfileTransportTypeMismatch_thenError() {
        when(deviceProfileCache.get(profileId)).thenReturn(profile(DeviceTransportType.MQTT, 1L));

        assertThatThrownBy(() -> service.toValidateResponse(msgBuilder().setDeviceProfileVersion(1).build(), DeviceTransportType.COAP))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different transport type");
    }

}
