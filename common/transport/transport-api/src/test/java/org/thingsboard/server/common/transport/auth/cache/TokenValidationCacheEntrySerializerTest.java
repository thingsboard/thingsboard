// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.gen.transport.TransportProtos;

import static org.assertj.core.api.Assertions.assertThat;

class TokenValidationCacheEntrySerializerTest {

    private final TokenValidationCacheEntrySerializer serializer = new TokenValidationCacheEntrySerializer();

    @Test
    void givenEntry_whenSerializeAndDeserialize_thenEqual() {
        TransportProtos.TokenValidationCacheEntryProto entry = TransportProtos.TokenValidationCacheEntryProto.newBuilder()
                .setCreatedTime(123456789L)
                .setResponse(TransportProtos.ValidateDeviceCredentialsResponseMsg.newBuilder()
                        .setDeviceInfo(TransportProtos.DeviceInfoProto.newBuilder()
                                .setTenantIdMSB(1L).setTenantIdLSB(2L)
                                .setDeviceIdMSB(3L).setDeviceIdLSB(4L)
                                .setDeviceName("device").setDeviceType("default")
                                .build())
                        .setCredentialsBody("token")
                        .setResult(TransportProtos.CredentialsValidationResult.VALIDATION_OK)
                        .build())
                .setRevoked(false)
                .build();

        byte[] bytes = serializer.serialize(entry);
        TransportProtos.TokenValidationCacheEntryProto result = serializer.deserialize("key", bytes);

        assertThat(result).isEqualTo(entry);
        assertThat(result.getRevoked()).isFalse();
    }

    @Test
    void givenRevokedMarker_whenRoundTrip_thenRevokedTrue() {
        TransportProtos.TokenValidationCacheEntryProto marker = TransportProtos.TokenValidationCacheEntryProto.newBuilder()
                .setCreatedTime(42L)
                .setRevoked(true)
                .build();

        TransportProtos.TokenValidationCacheEntryProto result = serializer.deserialize("key", serializer.serialize(marker));

        assertThat(result.getRevoked()).isTrue();
        assertThat(result).isEqualTo(marker);
    }

}
