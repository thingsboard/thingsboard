// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.service;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.DeviceProfile;
import org.thingsboard.server.common.data.DeviceTransportType;
import org.thingsboard.server.common.data.id.DeviceProfileId;
import org.thingsboard.server.common.transport.TransportDeviceProfileCache;
import org.thingsboard.server.common.transport.TransportServiceCallback;
import org.thingsboard.server.common.transport.auth.ValidateDeviceCredentialsResponse;
import org.thingsboard.server.common.transport.auth.cache.TokenValidationCacheService;
import org.thingsboard.server.gen.transport.TransportProtos;
import org.thingsboard.server.queue.TbQueueRequestTemplate;
import org.thingsboard.server.queue.common.TbProtoQueueMsg;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class DefaultTransportServiceTokenValidationTest {

    private final DeviceProfileId profileId = new DeviceProfileId(UUID.randomUUID());
    private TokenValidationCacheService cache;
    private TransportDeviceProfileCache profileCache;
    private TbQueueRequestTemplate<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>, TbProtoQueueMsg<TransportProtos.TransportApiResponseMsg>> template;
    private TransportServiceCallback<ValidateDeviceCredentialsResponse> callback;
    private DefaultTransportService service;

    @BeforeEach
    void setUp() {
        cache = mock(TokenValidationCacheService.class);
        profileCache = mock(TransportDeviceProfileCache.class);
        template = mock(TbQueueRequestTemplate.class);
        callback = mock(TransportServiceCallback.class);
        service = mock(DefaultTransportService.class, Mockito.CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(service, "tokenValidationCacheService", cache);
        ReflectionTestUtils.setField(service, "deviceProfileCache", profileCache);
        ReflectionTestUtils.setField(service, "transportApiRequestTemplate", template);
        ReflectionTestUtils.setField(service, "transportCallbackExecutor", MoreExecutors.newDirectExecutorService());
        var response = TransportProtos.TransportApiResponseMsg.newBuilder()
                .setValidateCredResponseMsg(TransportProtos.ValidateDeviceCredentialsResponseMsg.getDefaultInstance()).build();
        when(template.send(any())).thenReturn(Futures.immediateFuture(new TbProtoQueueMsg<>(UUID.randomUUID(), response)));
    }

    private TransportProtos.ValidateDeviceTokenRequestMsg request() {
        return TransportProtos.ValidateDeviceTokenRequestMsg.newBuilder().setToken("tok").build();
    }

    private TransportProtos.TokenValidationCacheEntryProto positiveEntry() {
        var msg = TransportProtos.ValidateDeviceCredentialsResponseMsg.newBuilder()
                .setCredentialsBody("tok")
                .setDeviceInfo(TransportProtos.DeviceInfoProto.newBuilder()
                        .setDeviceProfileIdMSB(profileId.getId().getMostSignificantBits())
                        .setDeviceProfileIdLSB(profileId.getId().getLeastSignificantBits())
                        .setDeviceName("dev").build())
                .build();
        return TransportProtos.TokenValidationCacheEntryProto.newBuilder().setResponse(msg).build();
    }

    private DeviceProfile profile(DeviceTransportType type) {
        DeviceProfile p = new DeviceProfile(profileId);
        p.setTransportType(type);
        return p;
    }

    @Test
    void givenCacheDisabled_whenProcess_thenKafkaPathAndNoLookup() {
        when(cache.isEnabled()).thenReturn(false);

        service.process(DeviceTransportType.DEFAULT, request(), callback);

        verify(cache, never()).lookup(any());
        verify(template).send(any());
        verify(cache).store(Mockito.eq("tok"), any());
        verify(callback).onSuccess(any());
    }

    @Test
    void givenPositiveHit_whenProcess_thenSuccessWithoutKafka() {
        when(cache.isEnabled()).thenReturn(true);
        when(cache.lookup("tok")).thenReturn(Optional.of(positiveEntry()));
        when(profileCache.get(profileId)).thenReturn(profile(DeviceTransportType.DEFAULT));

        service.process(DeviceTransportType.DEFAULT, request(), callback);

        ArgumentCaptor<ValidateDeviceCredentialsResponse> captor = ArgumentCaptor.forClass(ValidateDeviceCredentialsResponse.class);
        verify(callback).onSuccess(captor.capture());
        assertThat(captor.getValue().hasDeviceInfo()).isTrue();
        verify(template, never()).send(any());
    }

    @Test
    void givenPositiveHitWithProfileTransportMismatch_whenProcess_thenOnError() {
        when(cache.isEnabled()).thenReturn(true);
        when(cache.lookup("tok")).thenReturn(Optional.of(positiveEntry()));
        when(profileCache.get(profileId)).thenReturn(profile(DeviceTransportType.MQTT));

        service.process(DeviceTransportType.COAP, request(), callback);

        ArgumentCaptor<Throwable> captor = ArgumentCaptor.forClass(Throwable.class);
        verify(callback).onError(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(IllegalStateException.class);
        verify(callback, never()).onSuccess(any());
        verify(template, never()).send(any());
    }

    @Test
    void givenNegativeHit_whenProcess_thenEmptyResponseWithoutKafka() {
        when(cache.isEnabled()).thenReturn(true);
        when(cache.lookup("tok")).thenReturn(Optional.of(TransportProtos.TokenValidationCacheEntryProto.getDefaultInstance()));

        service.process(DeviceTransportType.DEFAULT, request(), callback);

        ArgumentCaptor<ValidateDeviceCredentialsResponse> captor = ArgumentCaptor.forClass(ValidateDeviceCredentialsResponse.class);
        verify(callback).onSuccess(captor.capture());
        assertThat(captor.getValue().hasDeviceInfo()).isFalse();
        verify(template, never()).send(any());
    }

    @Test
    void givenMiss_whenProcess_thenKafkaRequestCarriesProfileCachingFlag() {
        when(cache.isEnabled()).thenReturn(true);
        when(cache.lookup("tok")).thenReturn(Optional.empty());

        service.process(DeviceTransportType.DEFAULT, request(), callback);

        ArgumentCaptor<TbProtoQueueMsg<TransportProtos.TransportApiRequestMsg>> captor = ArgumentCaptor.forClass(TbProtoQueueMsg.class);
        verify(template).send(captor.capture());
        var sent = captor.getValue().getValue().getValidateTokenRequestMsg();
        assertThat(sent.getToken()).isEqualTo("tok");
        assertThat(sent.getSupportsDeviceProfileCaching()).isTrue();
        verify(cache).store(Mockito.eq("tok"), any());
    }

    @Test
    void givenSaturatedExecutor_whenProcess_thenOnErrorAndNoThrow() {
        when(cache.isEnabled()).thenReturn(true);
        ExecutorService rejecting = mock(ExecutorService.class);
        Mockito.doThrow(new RejectedExecutionException("full")).when(rejecting).execute(any());
        ReflectionTestUtils.setField(service, "transportCallbackExecutor", rejecting);

        service.process(DeviceTransportType.DEFAULT, request(), callback);

        verify(callback).onError(any(RejectedExecutionException.class));
    }

}
