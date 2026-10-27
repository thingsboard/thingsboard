// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.transport;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.transport.auth.cache.TokenValidationCacheService;
import org.thingsboard.server.dao.device.DeviceCredentialsEvictEvent;
import org.thingsboard.server.gen.transport.TransportProtos.TokenValidationCacheEntryProto;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class TokenValidationCacheEvictionListenerTest {

    private TbTransactionalCache<String, TokenValidationCacheEntryProto> cache;
    private TokenValidationCacheEvictionListener listener;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() {
        cache = mock(TbTransactionalCache.class);
        listener = new TokenValidationCacheEvictionListener(Optional.of(cache));
    }

    @Test
    void givenRotation_thenOldIdGetsRevokedMarker_newIdEvicted() {
        listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("newId", "oldId"));

        verify(cache).evict(TokenValidationCacheService.cacheKey("newId"));
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TokenValidationCacheEntryProto> value = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        verify(cache).put(key.capture(), value.capture());
        assertThat(key.getValue()).isEqualTo(TokenValidationCacheService.cacheKey("oldId")).startsWith("vt:");
        assertThat(value.getValue().getRevoked()).isTrue();
    }

    @Test
    void givenCreate_thenNewIdEvictedOnly() {
        listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("newId", null));

        verify(cache).evict(TokenValidationCacheService.cacheKey("newId"));
        verify(cache, never()).put(anyString(), any());
        verifyNoMoreInteractions(cache);
    }

    @Test
    void givenDelete_thenRevokedMarkerForDeletedId() {
        listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("delId", null, true));

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<TokenValidationCacheEntryProto> value = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        verify(cache).put(key.capture(), value.capture());
        assertThat(key.getValue()).isEqualTo(TokenValidationCacheService.cacheKey("delId"));
        assertThat(value.getValue().getRevoked()).isTrue();
        verify(cache, never()).evict(anyString());
    }

    @Test
    void givenNoCacheBean_thenNoop() {
        TokenValidationCacheEvictionListener noop = new TokenValidationCacheEvictionListener(Optional.empty());
        assertThatCode(() -> noop.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("a", "b"))).doesNotThrowAnyException();
    }

    @Test
    void givenCacheFailure_thenWarningCarriesHashedKeyButNoCredentialsId() {
        doThrow(new RuntimeException("redis down")).when(cache).evict(anyString());
        Logger logger = (Logger) LoggerFactory.getLogger(TokenValidationCacheEvictionListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("secret-new-token", "secret-old-token"));
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).hasSize(1);
        String message = appender.list.get(0).getFormattedMessage();
        assertThat(message).doesNotContain("secret-new-token").doesNotContain("secret-old-token")
                .contains(TokenValidationCacheService.cacheKey("secret-new-token"));
    }

    @Test
    void givenCacheFailure_thenNotPropagated() {
        doThrow(new RuntimeException("redis down")).when(cache).evict(anyString());
        assertThatCode(() -> listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("a", "b"))).doesNotThrowAnyException();
    }

}
