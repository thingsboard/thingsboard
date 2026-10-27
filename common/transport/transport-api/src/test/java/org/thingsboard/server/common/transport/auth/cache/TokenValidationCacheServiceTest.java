// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thingsboard.server.cache.TbCacheValueWrapper;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.gen.transport.TransportProtos.CredentialsValidationResult;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceInfoProto;
import org.thingsboard.server.gen.transport.TransportProtos.DeviceProfileProto;
import org.thingsboard.server.gen.transport.TransportProtos.TokenValidationCacheEntryProto;
import org.thingsboard.server.gen.transport.TransportProtos.ValidateDeviceCredentialsResponseMsg;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TokenValidationCacheServiceTest {

    private static final String TOKEN = "secret-token";
    private static final String KEY = TokenValidationCacheService.cacheKey(TOKEN);

    private TbTransactionalCache<String, TokenValidationCacheEntryProto> l2;
    private AtomicLong now;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        l2 = mock(TbTransactionalCache.class);
        now = new AtomicLong(1_000_000L);
    }

    private TokenValidationCacheService service(boolean enabled, int negativeTtlSec, int localTtlSec) {
        TokenValidationCacheService s = new TokenValidationCacheService(Optional.of(l2), enabled, negativeTtlSec, localTtlSec);
        s.clock = now::get;
        return s;
    }

    private static ValidateDeviceCredentialsResponseMsg positive() {
        return ValidateDeviceCredentialsResponseMsg.newBuilder()
                .setDeviceInfo(DeviceInfoProto.newBuilder().setDeviceName("dev").build())
                .setCredentialsBody("body")
                .setDeviceProfile(DeviceProfileProto.newBuilder().setName("profile").build())
                .setResult(CredentialsValidationResult.VALIDATION_OK)
                .build();
    }

    private static ValidateDeviceCredentialsResponseMsg withResult(CredentialsValidationResult result) {
        return ValidateDeviceCredentialsResponseMsg.newBuilder().setResult(result).build();
    }

    private static TokenValidationCacheEntryProto entry(long createdTime, ValidateDeviceCredentialsResponseMsg response) {
        return TokenValidationCacheEntryProto.newBuilder().setCreatedTime(createdTime).setResponse(response).build();
    }

    private void l2Returns(TokenValidationCacheEntryProto e) {
        TbCacheValueWrapper<TokenValidationCacheEntryProto> w = () -> e;
        when(l2.get(KEY)).thenReturn(w);
    }

    @Test
    void givenCacheDisabled_whenProcess_thenKafkaPathAndNoCacheCalls() {
        TokenValidationCacheService s = service(false, 30, 20);
        assertThat(s.isEnabled()).isFalse();
        assertThat(s.lookup(TOKEN)).isEmpty();
        s.store(TOKEN, positive());
        verifyNoInteractions(l2);
    }

    @Test
    void givenNoCacheBean_thenDisabled() {
        TokenValidationCacheService s = new TokenValidationCacheService(Optional.empty(), true, 30, 20);
        assertThat(s.isEnabled()).isFalse();
        assertThat(s.lookup(TOKEN)).isEmpty();
    }

    @Test
    void givenPositiveEntryInL2_whenLookup_thenHitAndL1Populated() {
        TokenValidationCacheEntryProto e = entry(now.get(), positive());
        l2Returns(e);
        TokenValidationCacheService s = service(true, 30, 20);

        assertThat(s.lookup(TOKEN)).contains(e);
        assertThat(s.lookup(TOKEN)).contains(e);

        verify(l2, times(1)).get(KEY);
    }

    @Test
    void givenL1TtlZero_whenLookup_thenAlwaysL2() {
        l2Returns(entry(now.get(), positive()));
        TokenValidationCacheService s = service(true, 30, 0);

        assertThat(s.lookup(TOKEN)).isPresent();
        assertThat(s.lookup(TOKEN)).isPresent();

        verify(l2, times(2)).get(KEY);
    }

    @Test
    void givenL2Miss_whenLookup_thenEmpty() {
        when(l2.get(KEY)).thenReturn(null);
        assertThat(service(true, 30, 20).lookup(TOKEN)).isEmpty();
    }

    @Test
    void givenNegativeEntryYoungerThanTtl_whenLookup_thenNegativeHit() {
        TokenValidationCacheEntryProto e = entry(now.get() - 29_000, withResult(CredentialsValidationResult.CREDENTIALS_NOT_FOUND));
        l2Returns(e);
        assertThat(service(true, 30, 0).lookup(TOKEN)).contains(e);
    }

    @Test
    void givenNegativeEntryOlderThanTtl_whenLookup_thenEmpty() {
        l2Returns(entry(now.get() - 31_000, withResult(CredentialsValidationResult.CREDENTIALS_NOT_FOUND)));
        assertThat(service(true, 30, 0).lookup(TOKEN)).isEmpty();
    }

    @Test
    void givenRevokedMarkerOlderThanNegativeTtl_whenLookup_thenStillHit() {
        TokenValidationCacheEntryProto marker = TokenValidationCacheService.revokedMarker(now.get() - 3_600_000);
        l2Returns(marker);
        Optional<TokenValidationCacheEntryProto> res = service(true, 30, 0).lookup(TOKEN);
        assertThat(res).contains(marker);
        assertThat(res.get().getRevoked()).isTrue();
        assertThat(res.get().getResponse().getResult()).isEqualTo(CredentialsValidationResult.CREDENTIALS_NOT_FOUND);
    }

    @Test
    void givenCacheThrows_whenLookup_thenFallsBackToKafka() {
        when(l2.get(anyString())).thenThrow(new RuntimeException("redis down"));
        TokenValidationCacheService s = service(true, 30, 20);
        assertThat(s.lookup(TOKEN)).isEmpty();
        assertThat(s.lookup(TOKEN)).isEmpty();
    }

    @Test
    void givenCacheThrows_whenStore_thenNoExceptionPropagates() {
        org.mockito.Mockito.doThrow(new RuntimeException("redis down")).when(l2).put(anyString(), any());
        TokenValidationCacheService s = service(true, 30, 20);
        assertThatCode(() -> s.store(TOKEN, positive())).doesNotThrowAnyException();
    }

    @Test
    void givenEmptyResponseWithoutResultCode_thenNotCached() {
        service(true, 30, 20).store(TOKEN, ValidateDeviceCredentialsResponseMsg.getDefaultInstance());
        verify(l2, never()).put(anyString(), any());
    }

    @Test
    void givenInternalErrorResult_thenNotCached() {
        service(true, 30, 20).store(TOKEN, withResult(CredentialsValidationResult.VALIDATION_INTERNAL_ERROR));
        verify(l2, never()).put(anyString(), any());
    }

    @Test
    void givenInternalErrorWithDeviceInfo_whenStore_thenNotCached() {
        ValidateDeviceCredentialsResponseMsg r = positive().toBuilder()
                .setResult(CredentialsValidationResult.VALIDATION_INTERNAL_ERROR).build();
        service(true, 30, 20).store(TOKEN, r);
        verify(l2, never()).put(anyString(), any());
    }

    @Test
    void givenNotFoundResult_thenNegativeCached() {
        service(true, 30, 20).store(TOKEN, withResult(CredentialsValidationResult.CREDENTIALS_NOT_FOUND));
        ArgumentCaptor<TokenValidationCacheEntryProto> c = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        verify(l2).put(org.mockito.ArgumentMatchers.eq(KEY), c.capture());
        assertThat(c.getValue().getCreatedTime()).isEqualTo(now.get());
        assertThat(c.getValue().getRevoked()).isFalse();
        assertThat(c.getValue().getResponse().getResult()).isEqualTo(CredentialsValidationResult.CREDENTIALS_NOT_FOUND);
    }

    @Test
    void givenNegativeTtlZero_thenNotFoundNotCached() {
        service(true, 0, 20).store(TOKEN, withResult(CredentialsValidationResult.CREDENTIALS_NOT_FOUND));
        verify(l2, never()).put(anyString(), any());
    }

    @Test
    void givenPositiveResponseWithProfile_whenStore_thenStoredEntryHasNoProfile() {
        service(true, 30, 20).store(TOKEN, positive());
        ArgumentCaptor<TokenValidationCacheEntryProto> c = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        verify(l2).put(org.mockito.ArgumentMatchers.eq(KEY), c.capture());
        ValidateDeviceCredentialsResponseMsg stored = c.getValue().getResponse();
        assertThat(stored.hasDeviceProfile()).isFalse();
        assertThat(stored.getDeviceInfo().getDeviceName()).isEqualTo("dev");
        assertThat(stored.getCredentialsBody()).isEqualTo("body");
        assertThat(c.getValue().getCreatedTime()).isEqualTo(now.get());
    }

    @Test
    void givenStoredEntry_whenLookup_thenServedFromL1WithoutL2() {
        TokenValidationCacheService s = service(true, 30, 20);
        s.store(TOKEN, positive());
        assertThat(s.lookup(TOKEN)).isPresent();
        verify(l2, never()).get(anyString());
    }

    @Test
    void cacheKey_isPrefixedSha256Hex() {
        assertThat(TokenValidationCacheService.cacheKey("abc"))
                .isEqualTo("vt:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
