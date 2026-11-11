// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.license;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.audit.ActionType;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.id.UserId;
import org.thingsboard.server.dao.audit.AuditLogService;
import org.thingsboard.server.dao.subscription.SubscriptionService;
import org.thingsboard.server.dao.subscription.TbClusterStore;
import org.thingsboard.server.service.security.model.SecurityUser;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class NonProductionConfirmationServiceTest {

    private final SubscriptionService subscriptionService = mock(SubscriptionService.class);
    private final TbClusterStore tbClusterStore = mock(TbClusterStore.class);
    private final AuditLogService auditLogService = mock(AuditLogService.class);
    private final NonProductionConfirmationService nonProductionConfirmationService =
            new NonProductionConfirmationService(subscriptionService, tbClusterStore, auditLogService);

    // Backs the getNonProductionConfirmedTs()/saveNonProductionConfirmedTs() stubs below with real state, so
    // that a save made through the mock is visible to a later read through the same mock - exactly what a
    // real tb_cluster row would do, and what the seeding tests need to observe.
    private final AtomicReference<Optional<Long>> storedConfirmedTs = new AtomicReference<>(Optional.empty());

    @BeforeEach
    public void setUp() {
        when(tbClusterStore.getNonProductionConfirmedTs()).thenAnswer(invocation -> storedConfirmedTs.get());
        doAnswer(invocation -> {
            long confirmedTs = invocation.getArgument(0);
            storedConfirmedTs.set(Optional.of(confirmedTs));
            return true;
        }).when(tbClusterStore).saveNonProductionConfirmedTs(anyLong());
        when(subscriptionService.isLicenseActivated()).thenReturn(true);
    }

    private void givenDevelopmentKeyDeployment() {
        when(subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)).thenReturn(true);
        when(subscriptionService.isNonProductionMode()).thenReturn(false);
    }

    private void givenKeylessDeployment() {
        when(subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)).thenReturn(true);
        when(subscriptionService.isNonProductionMode()).thenReturn(true);
    }

    private void givenProductionLicence() {
        when(subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)).thenReturn(false);
        when(subscriptionService.isNonProductionMode()).thenReturn(false);
    }

    private void givenUnactivatedAtStartup() {
        when(subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)).thenReturn(false);
        when(subscriptionService.isNonProductionMode()).thenReturn(false);
        storedConfirmedTs.set(Optional.empty());
    }

    private void givenConfirmedAt(long confirmedTs) {
        storedConfirmedTs.set(Optional.of(confirmedTs));
    }

    private SecurityUser sysAdmin() {
        SecurityUser user = new SecurityUser(new UserId(UUID.randomUUID()));
        user.setTenantId(TenantId.SYS_TENANT_ID);
        user.setCustomerId(new CustomerId(CustomerId.NULL_UUID));
        user.setEmail("sysadmin@thingsboard.io");
        return user;
    }

    @Test
    public void aFreshlyConfirmedDeploymentHasNotLapsed() {
        givenDevelopmentKeyDeployment();
        nonProductionConfirmationService.confirm(sysAdmin());

        assertThat(nonProductionConfirmationService.isLapsed()).isFalse();
    }

    @Test
    public void aDeploymentLapsesAfterTheConfirmationPeriod() {
        givenDevelopmentKeyDeployment();
        givenConfirmedAt(System.currentTimeMillis()
                - NonProductionConfirmationService.CONFIRMATION_PERIOD_MS - 1);

        assertThat(nonProductionConfirmationService.isLapsed()).isTrue();
    }

    @Test
    public void aKeylessDeploymentNeverLapses() {
        // Keyless is anonymous: there is no identity to record a declaration against, so it is out of scope
        // for the re-confirm loop even though it also reports development mode.
        givenKeylessDeployment();
        givenConfirmedAt(0L);

        assertThat(nonProductionConfirmationService.isLapsed()).isFalse();
        // The real guarantee is not just "not lapsed" - a keyless deployment must never be seeded or logged.
        verify(tbClusterStore, never()).saveNonProductionConfirmedTs(anyLong());
        verifyNoInteractions(auditLogService);
    }

    @Test
    public void aLockedNodeSeedsNothing() {
        // A locked node has no licence client, so isDevelopment() fails open to true while isNonProductionMode()
        // is false - both conditions hold, and seed() writes a cluster-wide row it has no business writing.
        when(subscriptionService.isDevelopment(TenantId.SYS_TENANT_ID)).thenReturn(true);
        when(subscriptionService.isNonProductionMode()).thenReturn(false);
        when(subscriptionService.isLicenseActivated()).thenReturn(false);

        assertThat(nonProductionConfirmationService.isLapsed()).isFalse();

        verify(tbClusterStore, never()).saveNonProductionConfirmedTs(anyLong());
    }

    @Test
    public void aLicensedProductionDeploymentNeverLapses() {
        givenProductionLicence();
        givenConfirmedAt(0L);

        assertThat(nonProductionConfirmationService.isLapsed()).isFalse();
        // The real guarantee is not just "not lapsed" - a licensed deployment must never be seeded or logged.
        verify(tbClusterStore, never()).saveNonProductionConfirmedTs(anyLong());
        verifyNoInteractions(auditLogService);
    }

    @Test
    public void aConfirmedDeploymentNeverReadsTheStore() {
        givenDevelopmentKeyDeployment();
        nonProductionConfirmationService.confirm(sysAdmin());

        assertThat(nonProductionConfirmationService.isLapsed()).isFalse();

        // confirm() never calls getNonProductionConfirmedTs() - it only writes, via saveNonProductionConfirmedTs()
        // - so with the cache warm from that write, the confirmed path in isLapsed() must not call it either.
        verify(tbClusterStore, never()).getNonProductionConfirmedTs();
    }

    @Test
    public void aPeerNodesConfirmationUnlocksThisNodeOnItsNextCheck() {
        // The cache is only trustworthy while confirmed; once lapsed it must be re-read on every check, so a
        // confirmation recorded by another node in the cluster is observed here without a restart.
        givenDevelopmentKeyDeployment();
        givenConfirmedAt(System.currentTimeMillis() - NonProductionConfirmationService.CONFIRMATION_PERIOD_MS - 1);

        assertThat(nonProductionConfirmationService.isLapsed()).isTrue();

        givenConfirmedAt(System.currentTimeMillis());

        assertThat(nonProductionConfirmationService.isLapsed()).isFalse();
    }

    @Test
    public void aDevelopmentKeyAppliedAtRuntimeGetsAFullPeriodRatherThanLapsingImmediately() {
        // The instance boots unactivated, so nothing is seeded at startup. A sysadmin then applies a DEV key
        // through the activation UI. Without lazy seeding confirmedTs stays 0 and the very next request
        // reports a lapse, locking the instance the instant its key is accepted.
        givenUnactivatedAtStartup();
        nonProductionConfirmationService.init();

        givenDevelopmentKeyDeployment();

        assertThat(nonProductionConfirmationService.isLapsed()).isFalse();
        assertThat(tbClusterStore.getNonProductionConfirmedTs()).isPresent();
    }

    @Test
    public void aStoreThatCannotBeReadAtStartupLeavesTheContextAliveAndTheVerdictToTheNextRead() {
        // The catch around the startup read is justified by a node booted against a not-yet-upgraded schema:
        // the column does not exist yet, and a @PostConstruct that throws takes the whole context down. The
        // second half of that rationale - that a failed warm-up degrades to exactly the "never confirmed"
        // behaviour - is what the later successful read below pins.
        doThrow(new RuntimeException("column non_production_confirmed_ts does not exist"))
                .doAnswer(invocation -> storedConfirmedTs.get())
                .when(tbClusterStore).getNonProductionConfirmedTs();

        assertThatCode(nonProductionConfirmationService::init).doesNotThrowAnyException();

        givenDevelopmentKeyDeployment();
        givenConfirmedAt(System.currentTimeMillis()
                - NonProductionConfirmationService.CONFIRMATION_PERIOD_MS - 1);

        assertThat(nonProductionConfirmationService.isLapsed()).isTrue();
    }

    @Test
    public void anEmptyReadAfterALapseDoesNotSilentlyReconfirmTheDeployment() {
        givenDevelopmentKeyDeployment();
        givenConfirmedAt(System.currentTimeMillis()
                - NonProductionConfirmationService.CONFIRMATION_PERIOD_MS - 1);

        assertThat(nonProductionConfirmationService.isLapsed()).isTrue();

        // The store momentarily answers empty - between a truncate and a reseed, say. Caching that as 0L
        // would send the next check into the seeding branch, which is the one way a lapsed deployment could
        // silently become a freshly confirmed one.
        storedConfirmedTs.set(Optional.empty());

        assertThat(nonProductionConfirmationService.isLapsed()).isTrue();
        verify(tbClusterStore, never()).saveNonProductionConfirmedTs(anyLong());
    }

    @Test
    public void seedingIsNotAuditLoggedBecauseNobodyDeclaredIt() {
        givenUnactivatedAtStartup();
        nonProductionConfirmationService.init();
        givenDevelopmentKeyDeployment();

        nonProductionConfirmationService.isLapsed();

        // Proves seeding actually happened (not just that isLapsed() did nothing at all) and that it was not
        // logged.
        assertThat(tbClusterStore.getNonProductionConfirmedTs()).isPresent();
        verifyNoInteractions(auditLogService);
    }

    @Test
    public void aConfirmationThatWasNotActuallyStoredFailsLoudlyInsteadOfBeingLogged() {
        // The shared tb_cluster row being missing must not produce an audit entry asserting a declaration
        // that was never recorded - a sysadmin told it failed can act; one shown success over a silent
        // no-op cannot.
        givenDevelopmentKeyDeployment();
        doReturn(false).when(tbClusterStore).saveNonProductionConfirmedTs(anyLong());

        assertThatThrownBy(() -> nonProductionConfirmationService.confirm(sysAdmin()))
                .isInstanceOf(IllegalStateException.class);

        verifyNoInteractions(auditLogService);
    }

    @Test
    public void aFailureToRecordTheAuditLogEntryDoesNotUndoAnAlreadyPersistedConfirmation() {
        // The write already persisted and already unlocked the instance by the time the audit call runs. A
        // sysadmin told the confirmation failed here would be told the opposite of what actually happened.
        givenDevelopmentKeyDeployment();
        doThrow(new RuntimeException("audit log unavailable")).when(auditLogService)
                .logEntityAction(any(), any(), any(), any(), any(), any(),
                        eq(ActionType.NON_PRODUCTION_CONFIRMED), isNull());

        nonProductionConfirmationService.confirm(sysAdmin());

        assertThat(nonProductionConfirmationService.isLapsed()).isFalse();
    }

    @Test
    public void aDeploymentWithNothingToConfirmIsRefusedRatherThanStampingTheSharedRow() {
        // Refusing here is not pedantry about a pointless call. The shared row is what isLapsed() reads to
        // decide whether a cluster has ever been seeded, and its emptiness is what earns a development key
        // applied at runtime a full period instead of an immediate lock. A confirmation accepted on a
        // production-licensed deployment fills that row in, so a development key applied to the same
        // instance more than one period later would be read as having lapsed long ago and would lock it on
        // its first request.
        givenProductionLicence();

        assertThatThrownBy(() -> nonProductionConfirmationService.confirm(sysAdmin()))
                .isInstanceOf(IllegalStateException.class);

        verify(tbClusterStore, never()).saveNonProductionConfirmedTs(anyLong());
        verifyNoInteractions(auditLogService);
    }

    @Test
    public void everyConfirmationIsRecordedRatherThanOverwritingTheLast() {
        // Each renewal must be its own recorded declaration, not just the latest one. The audit log is
        // append-only, so the history is retained without a table of our own.
        givenDevelopmentKeyDeployment();
        nonProductionConfirmationService.confirm(sysAdmin());
        nonProductionConfirmationService.confirm(sysAdmin());

        verify(auditLogService, times(2)).logEntityAction(any(), any(), any(), any(), any(), any(),
                eq(ActionType.NON_PRODUCTION_CONFIRMED), isNull());
    }

}
