// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.thingsboard.server.common.data.exception.ThingsboardErrorCode;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.dao.relation.RelationWriteLock;
import org.thingsboard.server.exception.ThingsboardRuntimeException;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The guard serializes a tenant's quota-consuming writes. It uses the TRY variant deliberately: waiting would
 * let one tenant's burst pile up requests that each hold an HTTP thread and a pooled connection, so contention
 * is reported as TOO_MANY_REQUESTS (HTTP 429) and the caller retries.
 */
class ExtensionQuotaGuardTest {

    private EntityManager entityManager;
    private Query query;
    private ExtensionQuotaGuard guard;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        guard = new ExtensionQuotaGuard(transactionManager);

        entityManager = mock(EntityManager.class);
        query = mock(Query.class);
        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter(any(Integer.class), any())).thenReturn(query);
        guard.entityManager = entityManager;

        tenantId = TenantId.fromUUID(UUID.randomUUID());
    }

    @Test
    void runsTheBodyWhenTheTenantLockIsAcquired() {
        when(query.getSingleResult()).thenReturn(Boolean.TRUE);

        String result = guard.run(tenantId, () -> "saved");

        assertThat(result).isEqualTo("saved");
        verify(entityManager).createNativeQuery(ExtensionQuotaGuard.TRY_LOCK_SQL);
        // keyed by tenant, so only that tenant's quota writes serialize
        verify(query).setParameter(1, RelationWriteLock.advisoryKey(tenantId));
    }

    @Test
    void rejectsInsteadOfWaitingWhenAnotherRequestHoldsTheLock() {
        when(query.getSingleResult()).thenReturn(Boolean.FALSE);
        AtomicBoolean bodyRan = new AtomicBoolean();

        assertThatThrownBy(() -> guard.run(tenantId, () -> {
            bodyRan.set(true);
            return null;
        }))
                .isInstanceOf(ThingsboardRuntimeException.class)
                .hasMessageContaining("retry")
                .extracting(e -> ((ThingsboardRuntimeException) e).getErrorCode())
                .isEqualTo(ThingsboardErrorCode.TOO_MANY_REQUESTS);

        // the quota check and the write it authorizes must not run unguarded
        assertThat(bodyRan).isFalse();
    }

}
