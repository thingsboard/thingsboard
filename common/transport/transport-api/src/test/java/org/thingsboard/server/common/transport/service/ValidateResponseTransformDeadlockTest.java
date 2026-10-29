// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.service;

import com.google.common.util.concurrent.SettableFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.common.util.ThingsBoardThreadFactory;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceProfileProvisionType;
import org.thingsboard.server.common.data.DeviceProfileType;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.auth.cache.TokenValidationCacheService;
import org.thingsboard.server.common.util.ProtoUtils;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Models DefaultTbQueueRequestTemplate: every response future is completed by one dedicated "main loop" thread.
 * The validate-response transform resolves an unseen device profile through a second request on the same template.
 * If the transform ran on the main loop thread, that nested request could never complete.
 */
@SuppressWarnings("unchecked")
class ValidateResponseTransformDeadlockTest {

    private final DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
    private ExecutorService mainLoop;
    private ExecutorService callbackExecutor;
    private final CountDownLatch responsesReleased = new CountDownLatch(1);
    private DefaultTransportService service;

    @BeforeEach
    void setUp() {
        mainLoop = Executors.newSingleThreadExecutor(ThingsBoardThreadFactory.forName("test-request-template-main-loop"));
        callbackExecutor = Executors.newFixedThreadPool(2, ThingsBoardThreadFactory.forName("test-transport-callback"));

        TbQueueRequestTemplate<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> template =
                mock(TbQueueRequestTemplate.class);
        when(template.send(any())).thenAnswer(invocation -> {
            TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg> request = invocation.getArgument(0);
            SettableFuture<TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> future = SettableFuture.create();
            mainLoop.execute(() -> {
                // like a real broker round-trip: the response arrives only after the caller has attached its transform
                awaitUninterruptibly(responsesReleased);
                future.set(new TbProtoQueueMsg<>(request.getKey(), respond(request.getValue())));
            });
            return future;
        });

        TokenValidationCacheService cacheService = mock(TokenValidationCacheService.class);
        when(cacheService.isEnabled()).thenReturn(false);

        DefaultTransportDeviceProfileCache profileCache = new DefaultTransportDeviceProfileCache();
        service = mock(DefaultTransportService.class, Mockito.CALLS_REAL_METHODS);
        profileCache.setTransportService(service);
        ReflectionTestUtils.setField(service, "tokenValidationCacheService", cacheService);
        ReflectionTestUtils.setField(service, "deviceProfileCache", profileCache);
        ReflectionTestUtils.setField(service, "transportApiRequestTemplate", template);
        ReflectionTestUtils.setField(service, "transportCallbackExecutor", callbackExecutor);
        ReflectionTestUtils.setField(service, "profileNegotiationEnabled", true);
    }

    @AfterEach
    void tearDown() {
        mainLoop.shutdownNow();
        callbackExecutor.shutdownNow();
    }

    @Test
    void givenUnseenProfile_whenValidateToken_thenNestedProfileFetchCompletes() throws Exception {
        CompletableFuture<ValidateDeviceCredentialsResponse> result = new CompletableFuture<>();

        service.process(DeviceTransportType.DEFAULT,
                TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken("token").build(), callback(result));
        responsesReleased.countDown();

        ValidateDeviceCredentialsResponse response = result.get(10, TimeUnit.SECONDS);
        assertThat(response.hasDeviceInfo()).isTrue();
        assertThat(response.getDeviceProfile()).isNotNull();
        assertThat(response.getDeviceProfile().getId()).isEqualTo(profileId);
    }

    @Test
    void givenUnseenProfile_whenValidateLwM2M_thenNestedProfileFetchCompletes() throws Exception {
        CompletableFuture<ValidateDeviceCredentialsResponse> result = new CompletableFuture<>();

        service.process(TransportProtos.ValidateDeviceLwM2MCredentialsRequestMsg.newBuilder().setCredentialsId("endpoint").build(), callback(result));
        responsesReleased.countDown();

        ValidateDeviceCredentialsResponse response = result.get(10, TimeUnit.SECONDS);
        assertThat(response.hasDeviceInfo()).isTrue();
        assertThat(response.getDeviceProfile()).isNotNull();
        assertThat(response.getDeviceProfile().getId()).isEqualTo(profileId);
    }

    private static TransportServiceCallback<ValidateDeviceCredentialsResponse> callback(CompletableFuture<ValidateDeviceCredentialsResponse> result) {
        return new TransportServiceCallback<>() {
            @Override
            public void onSuccess(ValidateDeviceCredentialsResponse msg) {
                result.complete(msg);
            }

            @Override
            public void onError(Throwable e) {
                result.completeExceptionally(e);
            }
        };
    }

    private static void awaitUninterruptibly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private TransportProtos.TransportApiResponseMsg respond(TransportProtos.TransportApiRequestMsg request) {
        if (request.hasEntityProfileRequestMsg()) {
            return TransportProtos.TransportApiResponseMsg.newBuilder()
                    .setEntityProfileResponseMsg(TransportProtos.GetEntityProfileResponseMsg.newBuilder()
                            .setDeviceProfile(ProtoUtils.toProto(profile())))
                    .build();
        }
        // a profile-caching aware core omits the device profile from the validation response
        return TransportProtos.TransportApiResponseMsg.newBuilder()
                .setValidateCredResponseMsg(TransportProtos.ValidateDeviceCredentialsResponseMsg.newBuilder()
                        .setDeviceInfo(TransportProtos.DeviceInfoProto.newBuilder()
                                .setTenantIdMSB(1).setTenantIdLSB(2)
                                .setDeviceIdMSB(3).setDeviceIdLSB(4)
                                .setDeviceProfileIdMSB(profileId.getId().getMostSignificantBits())
                                .setDeviceProfileIdLSB(profileId.getId().getLeastSignificantBits())
                                .setDeviceName("device").setDeviceType("default"))
                        .setDeviceProfileVersion(1)
                        .setResult(TransportProtos.CredentialsValidationResult.VALIDATION_OK))
                .build();
    }

    private DeviceProfile profile() {
        DeviceProfile profile = new DeviceProfile(profileId);
        profile.setTenantId(TenantId.fromUUID(new UUID(1, 2)));
        profile.setName("profile");
        profile.setType(DeviceProfileType.DEFAULT);
        profile.setTransportType(DeviceTransportType.DEFAULT);
        profile.setProvisionType(DeviceProfileProvisionType.DISABLED);
        profile.setVersion(1L);
        return profile;
    }

}
