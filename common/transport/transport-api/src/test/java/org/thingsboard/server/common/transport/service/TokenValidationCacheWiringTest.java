// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.auth.cache.InMemoryTransactionalCache;
import org.thingsboard.server.common.transport.auth.cache.TokenValidationCacheService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Real {@link TokenValidationCacheService} (real L1, real I/O executor, map-backed L2) wired into DefaultTransportService.
 */
@SuppressWarnings("unchecked")
class TokenValidationCacheWiringTest {

    private static final String TOKEN = "wiring-token";

    private final DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
    private InMemoryTransactionalCache<String, TransportProtos.TokenValidationCacheEntryProto> l2;
    private TokenValidationCacheService cacheService;
    private TbQueueRequestTemplate<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> template;
    private DefaultTransportService service;

    @BeforeEach
    void setUp() {
        l2 = new InMemoryTransactionalCache<>();
        cacheService = new TokenValidationCacheService(Optional.of(l2), "redis", true, 30, 20);

        DeviceProfile profile = new DeviceProfile(profileId);
        profile.setTransportType(DeviceTransportType.DEFAULT);
        TransportDeviceProfileCache profileCache = mock(TransportDeviceProfileCache.class);
        when(profileCache.getOrCreate(any(), any())).thenReturn(profile);
        when(profileCache.get(profileId)).thenReturn(profile);

        template = mock(TbQueueRequestTemplate.class);
        var coreResponse = TransportProtos.TransportApiResponseMsg.newBuilder()
                .setValidateCredResponseMsg(TransportProtos.ValidateDeviceCredentialsResponseMsg.newBuilder()
                        .setCredentialsBody(TOKEN)
                        .setDeviceInfo(TransportProtos.DeviceInfoProto.newBuilder()
                                .setDeviceProfileIdMSB(profileId.getId().getMostSignificantBits())
                                .setDeviceProfileIdLSB(profileId.getId().getLeastSignificantBits())
                                .setDeviceName("dev"))
                        .setDeviceProfile(TransportProtos.DeviceProfileProto.newBuilder().setName("profile"))
                        .setResult(TransportProtos.CredentialsValidationResult.VALIDATION_OK))
                .build();
        when(template.send(any())).thenReturn(Futures.immediateFuture(new TbProtoQueueMsg<>(UUID.randomUUID(), coreResponse)));

        service = mock(DefaultTransportService.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "tokenValidationCacheService", cacheService);
        ReflectionTestUtils.setField(service, "deviceProfileCache", profileCache);
        ReflectionTestUtils.setField(service, "transportApiRequestTemplate", template);
        ReflectionTestUtils.setField(service, "transportCallbackExecutor", MoreExecutors.newDirectExecutorService());
    }

    @AfterEach
    void tearDown() {
        cacheService.destroy();
    }

    @Test
    void givenRepeatedValidation_whenCacheEnabled_thenSecondServedFromCacheWithoutCoreRequest() throws Exception {
        ValidateDeviceCredentialsResponse first = validate();
        assertThat(first.hasDeviceInfo()).isTrue();
        verify(template, times(1)).send(any());
        await().atMost(5, TimeUnit.SECONDS).until(() -> l2.map.containsKey(TokenValidationCacheService.cacheKey(TOKEN)));
        assertThat(l2.map.get(TokenValidationCacheService.cacheKey(TOKEN)).getResponse().hasDeviceProfile()).isFalse();

        ValidateDeviceCredentialsResponse second = validate();
        assertThat(second.hasDeviceInfo()).isTrue();
        assertThat(second.getDeviceInfo().getDeviceName()).isEqualTo("dev");
        assertThat(second.getDeviceProfile().getId()).isEqualTo(profileId);

        ValidateDeviceCredentialsResponse third = validate();
        assertThat(third.hasDeviceInfo()).isTrue();

        verify(template, times(1)).send(any());
        // first: L2 miss, second: L2 hit populating L1, third: L1 hit
        assertThat(l2.getCount.get()).isEqualTo(2);
    }

    private ValidateDeviceCredentialsResponse validate() throws Exception {
        CompletableFuture<ValidateDeviceCredentialsResponse> result = new CompletableFuture<>();
        service.process(DeviceTransportType.DEFAULT, TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken(TOKEN).build(),
                new TransportServiceCallback<>() {
                    @Override
                    public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                        result.complete(msg);
                    }

                    @Override
                    public void onError(Throwable e) {
                        result.completeExceptionally(e);
                    }
                });
        return result.get(5, TimeUnit.SECONDS);
    }

}
