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
import org.mockito.InOrder;
import org.slf4j.LoggerFactory;
import org.thingsboard.server.cache.TbTransactionalCache;
import org.thingsboard.server.common.transport.auth.cache.TokenValidationCacheService;
import org.thingsboard.server.dao.device.DeviceCredentialsEvictEvent;
import org.thingsboard.server.gen.transport.TransportProtos.TokenValidationCacheEntryProto;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
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
    void givenRotation_thenRevokedMarkerForOldIdWrittenBeforeTombstoneForNewId() {
        listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("newId", "oldId", false));

        ArgumentCaptor<TokenValidationCacheEntryProto> revoked = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        ArgumentCaptor<TokenValidationCacheEntryProto> tombstone = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        InOrder order = inOrder(cache);
        order.verify(cache).put(eq(TokenValidationCacheService.cacheKey("oldId")), revoked.capture(), eq(0L));
        order.verify(cache).put(eq(TokenValidationCacheService.cacheKey("newId")), tombstone.capture(),
                eq(TokenValidationCacheService.TOMBSTONE_TTL_SEC));
        assertThat(revoked.getValue().getRevoked()).isTrue();
        assertThat(tombstone.getValue().getTombstone()).isTrue();
        assertThat(tombstone.getValue().getRevoked()).isFalse();
        verify(cache, never()).evict(anyString());
    }

    @Test
    void givenCreate_thenNewIdGetsShortLivedTombstoneOnly() {
        listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("newId", null, false));

        ArgumentCaptor<TokenValidationCacheEntryProto> value = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        verify(cache).put(eq(TokenValidationCacheService.cacheKey("newId")), value.capture(),
                eq(TokenValidationCacheService.TOMBSTONE_TTL_SEC));
        assertThat(value.getValue().getTombstone()).isTrue();
        verifyNoMoreInteractions(cache);
    }

    @Test
    void givenDelete_thenRevokedMarkerForDeletedIdOnly() {
        listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("delId", null, true));

        ArgumentCaptor<TokenValidationCacheEntryProto> value = ArgumentCaptor.forClass(TokenValidationCacheEntryProto.class);
        verify(cache).put(eq(TokenValidationCacheService.cacheKey("delId")), value.capture(), eq(0L));
        assertThat(value.getValue().getRevoked()).isTrue();
        verifyNoMoreInteractions(cache);
    }

    @Test
    void givenRevokedMarkerWriteFails_thenTombstoneIsStillWritten() {
        doThrow(new RuntimeException("redis down"))
                .when(cache).put(eq(TokenValidationCacheService.cacheKey("oldId")), any(), anyLong());

        assertThatCode(() -> listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("newId", "oldId", false)))
                .doesNotThrowAnyException();

        verify(cache).put(eq(TokenValidationCacheService.cacheKey("newId")), any(),
                eq(TokenValidationCacheService.TOMBSTONE_TTL_SEC));
    }

    @Test
    void givenNoCacheBean_thenNoop() {
        TokenValidationCacheEvictionListener noop = new TokenValidationCacheEvictionListener(Optional.empty());
        assertThatCode(() -> noop.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("a", "b", false))).doesNotThrowAnyException();
    }

    @Test
    void givenCacheFailure_thenWarningCarriesHashedKeyButNoCredentialsId() {
        doThrow(new RuntimeException("redis down")).when(cache).put(anyString(), any(), anyLong());
        Logger logger = (Logger) LoggerFactory.getLogger(TokenValidationCacheEvictionListener.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("secret-new-token", "secret-old-token", false));
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).hasSize(2);
        for (ILoggingEvent event : appender.list) {
            assertThat(event.getFormattedMessage()).doesNotContain("secret-new-token").doesNotContain("secret-old-token");
        }
        assertThat(appender.list.get(0).getFormattedMessage()).contains(TokenValidationCacheService.cacheKey("secret-old-token"));
        assertThat(appender.list.get(1).getFormattedMessage()).contains(TokenValidationCacheService.cacheKey("secret-new-token"));
    }

    @Test
    void givenCacheFailure_thenNotPropagated() {
        doThrow(new RuntimeException("redis down")).when(cache).put(anyString(), any(), anyLong());
        assertThatCode(() -> listener.onDeviceCredentialsEvict(new DeviceCredentialsEvictEvent("a", "b", false))).doesNotThrowAnyException();
    }

}
