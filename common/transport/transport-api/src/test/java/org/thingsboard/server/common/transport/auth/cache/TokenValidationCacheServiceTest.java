// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.auth.cache;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TokenValidationCacheServiceTest {

    private static final String TOKEN = "secret-token";
    private static final String KEY = TokenValidationCacheService.cacheKey(TOKEN);
    private static final Executor REJECTING = r -> {
        throw new RejectedExecutionException("saturated");
    };

    private TbTransactionalCache<String, TokenValidationCacheEntryProto> l2;
    private AtomicLong now;
    private final List<TokenValidationCacheService> created = new ArrayList<>();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        l2 = mock(TbTransactionalCache.class);
        now = new AtomicLong(1_000_000L);
    }

    @AfterEach
    void tearDown() {
        created.forEach(TokenValidationCacheService::destroy);
    }

    private TokenValidationCacheService service(boolean enabled, int negativeTtlSec, int localTtlSec) {
        return service("redis", enabled, negativeTtlSec, localTtlSec);
    }

    private TokenValidationCacheService service(String cacheType, boolean enabled, int negativeTtlSec, int localTtlSec) {
        TokenValidationCacheService s = new TokenValidationCacheService(Optional.of(l2), cacheType, enabled, negativeTtlSec, localTtlSec);
        created.add(s);
        s.clock = now::get;
        s.ioExecutor = MoreExecutors.directExecutor();
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

    private void verifyNothingWritten() {
        verify(l2, never()).put(anyString(), any());
        verify(l2, never()).putIfAbsent(anyString(), any());
    }

    @Test
    void givenCacheDisabled_whenProcess_thenKafkaPathAndNoCacheCalls() throws Exception {
        TokenValidationCacheService s = service(false, 30, 20);
        assertThat(s.isEnabled()).isFalse();
        assertThat(s.lookup(TOKEN)).isEmpty();
        assertThat(s.lookupAsync(TOKEN).get()).isEmpty();
        s.store(TOKEN, positive());
        verifyNoInteractions(l2);
    }

    @Test
    void givenNoCacheBean_thenDisabled() {
        TokenValidationCacheService s = new TokenValidationCacheService(Optional.empty(), "redis", true, 30, 20);
        created.add(s);
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
    void givenCaffeineCacheType_whenLookup_thenNoL1() {
        l2Returns(entry(now.get(), positive()));
        TokenValidationCacheService s = service("caffeine", true, 30, 20);

        assertThat(s.lookup(TOKEN)).isPresent();
        assertThat(s.lookup(TOKEN)).isPresent();

        verify(l2, times(2)).get(KEY);
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
    void givenNegativeEntryInL2_whenLookupTwice_thenNotKeptInL1() {
        TokenValidationCacheEntryProto e = entry(now.get(), withResult(CredentialsValidationResult.CREDENTIALS_NOT_FOUND));
        l2Returns(e);
        TokenValidationCacheService s = service(true, 30, 20);

        assertThat(s.lookup(TOKEN)).contains(e);
        assertThat(s.lookup(TOKEN)).contains(e);

        verify(l2, times(2)).get(KEY);
    }

    @Test
    void givenRevokedMarkerInL2_whenLookupTwice_thenKeptInL1() {
        l2Returns(TokenValidationCacheService.revokedMarker(now.get()));
        TokenValidationCacheService s = service(true, 30, 20);

        assertThat(s.lookup(TOKEN)).isPresent();
        assertThat(s.lookup(TOKEN)).isPresent();

        verify(l2, times(1)).get(KEY);
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
        doThrow(new RuntimeException("redis down")).when(l2).putIfAbsent(anyString(), any());
        TokenValidationCacheService s = service(true, 30, 20);
        assertThatCode(() -> s.store(TOKEN, positive())).doesNotThrowAnyException();
    }

    @Test
    void givenEmptyResponseWithoutResultCode_thenNotCached() {
        service(true, 30, 20).store(TOKEN, ValidateDeviceCredentialsResponseMsg.getDefaultInstance());
        verifyNothingWritten();
    }

    @Test
    void givenInternalErrorResult_thenNotCached() {
        service(true, 30, 20).store(TOKEN, withResult(CredentialsValidationResult.VALIDATION_INTERNAL_ERROR));
        verifyNothingWritten();
    }

    @Test
    void givenInternalErrorWithDeviceInfo_whenStore_thenNotCached() {
        ValidateDeviceCredentialsResponseMsg r = positive().toBuilder()
                .setResult(CredentialsValidationResult.VALIDATION_INTERNAL_ERROR).build();
        service(true, 30, 20).store(TOKEN, r);
        verifyNothingWritten();
    }

    @Test
    void givenNotFoundResult_thenNegativeCachedAsUpsert() {
        service(true, 30, 20).store(TOKEN, withResult(CredentialsValidationResult.CREDENTIALS_NOT_FOUND));
        ArgumentCaptor<TokenValidationCacheEntryProto> c = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        verify(l2).put(eq(KEY), c.capture());
        verify(l2, never()).putIfAbsent(anyString(), any());
        assertThat(c.getValue().getCreatedTime()).isEqualTo(now.get());
        assertThat(c.getValue().getRevoked()).isFalse();
        assertThat(c.getValue().getResponse().getResult()).isEqualTo(CredentialsValidationResult.CREDENTIALS_NOT_FOUND);
    }

    @Test
    void givenNegativeTtlZero_thenNotFoundNotCached() {
        service(true, 0, 20).store(TOKEN, withResult(CredentialsValidationResult.CREDENTIALS_NOT_FOUND));
        verifyNothingWritten();
    }

    @Test
    void givenPositiveResponseWithProfile_whenStore_thenPutIfAbsentWithoutProfile() {
        service(true, 30, 20).store(TOKEN, positive());
        ArgumentCaptor<TokenValidationCacheEntryProto> c = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        verify(l2).putIfAbsent(eq(KEY), c.capture());
        verify(l2, never()).put(anyString(), any());
        ValidateDeviceCredentialsResponseMsg stored = c.getValue().getResponse();
        assertThat(stored.hasDeviceProfile()).isFalse();
        assertThat(stored.getDeviceInfo().getDeviceName()).isEqualTo("dev");
        assertThat(stored.getCredentialsBody()).isEqualTo("body");
        assertThat(c.getValue().getCreatedTime()).isEqualTo(now.get());
    }

    @Test
    void givenRevokedMarkerPresent_whenStorePositive_thenMarkerIntact() {
        InMemoryTransactionalCache<String, TokenValidationCacheEntryProto> store = new InMemoryTransactionalCache<>();
        TokenValidationCacheEntryProto marker = TokenValidationCacheService.revokedMarker(now.get());
        store.map.put(KEY, marker);
        TokenValidationCacheService s = new TokenValidationCacheService(Optional.of(store), "redis", true, 30, 20);
        created.add(s);
        s.ioExecutor = MoreExecutors.directExecutor();

        // a positive response produced before the rotation commit arrives after the core wrote the marker
        s.store(TOKEN, positive());

        assertThat(store.map.get(KEY)).isEqualTo(marker);
        assertThat(s.lookup(TOKEN)).contains(marker);
    }

    @Test
    void givenStoredPositive_whenLookup_thenReadFromL2NotFromStoreSideL1() {
        InMemoryTransactionalCache<String, TokenValidationCacheEntryProto> store = new InMemoryTransactionalCache<>();
        TokenValidationCacheService s = new TokenValidationCacheService(Optional.of(store), "redis", true, 30, 20);
        created.add(s);
        s.ioExecutor = MoreExecutors.directExecutor();

        s.store(TOKEN, positive());
        assertThat(s.lookup(TOKEN)).isPresent();
        assertThat(s.lookup(TOKEN)).isPresent();

        assertThat(store.getCount.get()).isEqualTo(1);
    }

    @Test
    void givenRejectingExecutor_whenLookupAsync_thenImmediateMissWithoutCacheAccess() throws Exception {
        TokenValidationCacheService s = service(true, 30, 20);
        s.ioExecutor = REJECTING;

        ListenableFuture<Optional<TokenValidationCacheEntryProto>> f = s.lookupAsync(TOKEN);

        assertThat(f.isDone()).isTrue();
        assertThat(f.get()).isEmpty();
        verifyNoInteractions(l2);
    }

    @Test
    void givenRejectingExecutor_whenStore_thenDroppedWithoutException() {
        TokenValidationCacheService s = service(true, 30, 20);
        s.ioExecutor = REJECTING;

        assertThatCode(() -> s.store(TOKEN, positive())).doesNotThrowAnyException();
        verifyNoInteractions(l2);
    }

    @Test
    void givenSlowCacheSaturatingRealExecutor_whenLookupAsync_thenImmediateMiss() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        when(l2.get(anyString())).thenAnswer(inv -> {
            release.await();
            return null;
        });
        TokenValidationCacheService s = new TokenValidationCacheService(Optional.of(l2), "redis", true, 30, 20);
        created.add(s);
        try {
            for (int i = 0; i < TokenValidationCacheService.IO_THREADS + TokenValidationCacheService.IO_QUEUE_CAPACITY; i++) {
                s.lookupAsync("token-" + i);
            }

            ListenableFuture<Optional<TokenValidationCacheEntryProto>> overflow = s.lookupAsync(TOKEN);

            assertThat(overflow.isDone()).isTrue();
            assertThat(overflow.get()).isEmpty();
        } finally {
            release.countDown();
        }
    }

    @Test
    void givenPositiveEntry_whenLookupAsyncOnRealExecutor_thenHit() throws Exception {
        TokenValidationCacheEntryProto e = entry(now.get(), positive());
        l2Returns(e);
        TokenValidationCacheService s = new TokenValidationCacheService(Optional.of(l2), "redis", true, 30, 20);
        created.add(s);

        assertThat(s.lookupAsync(TOKEN).get(5, TimeUnit.SECONDS)).contains(e);
    }

    @Test
    void cacheKey_isPrefixedSha256Hex() {
        assertThat(TokenValidationCacheService.cacheKey("abc"))
                .isEqualTo("vt:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}
