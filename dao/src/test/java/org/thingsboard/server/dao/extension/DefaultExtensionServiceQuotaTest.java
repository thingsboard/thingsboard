// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionContainerSpec;
import org.thingsboard.server.common.data.extension.ExtensionQuota;
import org.thingsboard.server.common.data.extension.ExtensionSizeAvailability;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultExtensionServiceQuotaTest {

    private ExtensionDao extensionDao;
    private ExtensionQuotaGuard quotaGuard;
    private DefaultExtensionService service;
    private DefaultTenantProfileConfiguration profileConfiguration;
    private TenantId tenantId;

    @BeforeEach
    void setUp() {
        extensionDao = mock(ExtensionDao.class);
        // The guard's own serialization is covered by ExtensionQuotaGuardTest; here it just runs the body
        // so these tests exercise the quota arithmetic itself.
        quotaGuard = mock(ExtensionQuotaGuard.class);
        when(quotaGuard.run(any(), any())).thenAnswer(inv -> ((Supplier<?>) inv.getArgument(1)).get());
        ApiLimitService apiLimitService = mock(ApiLimitService.class);
        service = new DefaultExtensionService(extensionDao, null, null, apiLimitService, quotaGuard);
        tenantId = TenantId.fromUUID(UUID.randomUUID());
        profileConfiguration = new DefaultTenantProfileConfiguration();
        // a fresh profile ships with room for one extension of the default tier (covered by
        // defaultProfileFitsExactlyOneExtensionOfTheDefaultTier);
        // the cases below drive the limits themselves, so start every one of them from "unlimited"
        profileConfiguration.setMaxExtensions(0);
        profileConfiguration.setMaxExtensionCpuMillicores(0);
        profileConfiguration.setMaxExtensionMemoryMb(0);
        when(apiLimitService.getLimit(eq(tenantId), any())).thenAnswer(invocation -> {
            Function<DefaultTenantProfileConfiguration, Number> extractor = invocation.getArgument(1);
            return Math.max(0, extractor.apply(profileConfiguration).longValue());
        });
    }

    private static Extension extension(ResourceSize size, int replicas, ExtensionState state) {
        Extension ext = new Extension();
        ext.setId(new ExtensionId(UUID.randomUUID()));
        ext.setState(state);
        ExtensionContainerSpec spec = new ExtensionContainerSpec();
        spec.setReplicas(replicas);
        ResourceSpec resources = new ResourceSpec();
        resources.setSize(size);
        spec.setResources(resources);
        ext.setContainerSpec(spec);
        return ext;
    }

    private void tenantHas(Extension... extensions) {
        when(extensionDao.findAllByTenantId(eq(tenantId), any()))
                .thenReturn(new PageData<>(List.of(extensions), 1, extensions.length, false));
    }

    @Test
    void computesDemandFromSizeAndReplicas() {
        Extension ext = extension(ResourceSize.MEDIUM, 3, ExtensionState.ACTIVE); // limits: 1 cpu / 512Mi
        assertThat(service.cpuLimitMillicores(ext)).isEqualTo(3000);
        assertThat(service.memoryLimitBytes(ext)).isEqualTo(3L * 512 * 1024 * 1024);
    }

    // The Docker backend has no load balancer behind one container name, so it runs exactly one
    // container whatever `replicas` says. Charging the requested count there rejects deploys while the
    // capacity sits physically idle, and reports usage no running container accounts for.
    @Test
    void dockerModeChargesForTheSingleContainerItActuallyRuns() {
        ReflectionTestUtils.setField(service, "extensionsMode", "docker-compose");
        Extension ext = extension(ResourceSize.MEDIUM, 3, ExtensionState.ACTIVE); // limits: 1 cpu / 512Mi

        assertThat(service.cpuLimitMillicores(ext)).isEqualTo(1000);
        assertThat(service.memoryLimitBytes(ext)).isEqualTo(512L * 1024 * 1024);
    }

    @Test
    void kubernetesModesChargeForEveryRequestedReplica() {
        ReflectionTestUtils.setField(service, "extensionsMode", "private-cloud");
        Extension ext = extension(ResourceSize.MEDIUM, 3, ExtensionState.ACTIVE);

        assertThat(service.cpuLimitMillicores(ext)).isEqualTo(3000);
        assertThat(service.memoryLimitBytes(ext)).isEqualTo(3L * 512 * 1024 * 1024);
    }

    @Test
    void missingResourcesFallBackToDefaultSize() {
        Extension ext = extension(null, 1, ExtensionState.ACTIVE);
        ext.getContainerSpec().setResources(null);
        // Omitted resources resolve to ResourceSize.DEFAULT; assert against the tier, not its numbers,
        // so moving the default does not silently leave this test checking the wrong thing.
        assertThat(service.cpuLimitMillicores(ext))
                .isEqualTo(service.cpuLimitMillicores(extension(ResourceSize.DEFAULT, 1, null)));
        assertThat(service.memoryLimitBytes(ext))
                .isEqualTo(service.memoryLimitBytes(extension(ResourceSize.DEFAULT, 1, null)));
    }

    @Test
    void noQuotaConfiguredSkipsTheScan() {
        assertThatCode(() -> service.validateResourceCapacity(tenantId, extension(ResourceSize.XXLARGE, 20, null)))
                .doesNotThrowAnyException();
        verify(extensionDao, never()).findAllByTenantId(any(), any());
    }

    @Test
    void rejectsCreateWhenCpuQuotaExceeded() {
        profileConfiguration.setMaxExtensionCpuMillicores(1000);
        tenantHas(extension(ResourceSize.MEDIUM, 1, ExtensionState.ACTIVE)); // 1000m used
        assertThatThrownBy(() -> service.validateResourceCapacity(tenantId,
                extension(ResourceSize.XSMALL, 1, null))) // +250m
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("CPU limit reached");
    }

    @Test
    void rejectsCreateWhenMemoryQuotaExceeded() {
        profileConfiguration.setMaxExtensionMemoryMb(512);
        tenantHas(extension(ResourceSize.SMALL, 2, ExtensionState.ACTIVE)); // 512 MB used
        assertThatThrownBy(() -> service.validateResourceCapacity(tenantId,
                extension(ResourceSize.XSMALL, 1, null))) // +128 MB
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("memory limit reached");
    }

    @Test
    void allowsCreateWithinQuota() {
        profileConfiguration.setMaxExtensionCpuMillicores(2000);
        profileConfiguration.setMaxExtensionMemoryMb(1024);
        tenantHas(extension(ResourceSize.MEDIUM, 1, ExtensionState.ACTIVE)); // 1000m / 512 MB used
        assertThatCode(() -> service.validateResourceCapacity(tenantId,
                extension(ResourceSize.MEDIUM, 1, null))) // +1000m / +512 MB, exactly at the quota
                .doesNotThrowAnyException();
    }

    @Test
    void stoppedExtensionsDoNotOccupyQuota() {
        profileConfiguration.setMaxExtensionCpuMillicores(1000);
        tenantHas(extension(ResourceSize.XXLARGE, 20, ExtensionState.STOPPED));
        assertThatCode(() -> service.validateResourceCapacity(tenantId,
                extension(ResourceSize.MEDIUM, 1, null)))
                .doesNotThrowAnyException();
    }

    @Test
    void candidateItselfIsExcludedFromOccupied() {
        profileConfiguration.setMaxExtensionCpuMillicores(1000);
        Extension existing = extension(ResourceSize.MEDIUM, 1, ExtensionState.STOPPED); // starting this one
        tenantHas(existing);
        assertThatCode(() -> service.validateResourceCapacity(tenantId, existing)).doesNotThrowAnyException();
    }

    /* --- Start path --- */

    // Updates of STOPPED rows skip the capacity check ("re-checked on start"), so the start must
    // check the committed row, not the caller's snapshot — or a racing save bypasses the quota.
    @Test
    void startReChecksCapacityAgainstTheFreshRow() {
        profileConfiguration.setMaxExtensionCpuMillicores(1000);
        Extension stale = extension(ResourceSize.SMALL, 1, ExtensionState.STOPPED); // what the controller read
        Extension fresh = extension(ResourceSize.XXLARGE, 20, ExtensionState.STOPPED); // what a racing save committed
        fresh.setId(stale.getId());
        when(extensionDao.findByTenantIdAndId(tenantId, stale.getId())).thenReturn(fresh);
        tenantHas(fresh);

        assertThatThrownBy(() -> service.startWithinQuota(tenantId, stale))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("CPU limit reached");
        verify(extensionDao, never()).save(any(), any());
    }

    @Test
    void startRejectsWhenTheRowIsNoLongerStopped() {
        Extension stale = extension(ResourceSize.SMALL, 1, ExtensionState.STOPPED);
        Extension fresh = extension(ResourceSize.SMALL, 1, ExtensionState.ACTIVE);
        fresh.setId(stale.getId());
        when(extensionDao.findByTenantIdAndId(tenantId, stale.getId())).thenReturn(fresh);

        assertThatThrownBy(() -> service.startWithinQuota(tenantId, stale))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("STOPPED");
        verify(extensionDao, never()).save(any(), any());
    }

    @Test
    void startWithinQuotaFlipsTheFreshRowToProvisioning() {
        Extension fresh = extension(ResourceSize.SMALL, 1, ExtensionState.STOPPED);
        when(extensionDao.findByTenantIdAndId(tenantId, fresh.getId())).thenReturn(fresh);
        tenantHas(fresh);

        service.startWithinQuota(tenantId, fresh);

        verify(extensionDao).save(eq(tenantId), org.mockito.ArgumentMatchers.argThat(
                e -> e.getState() == ExtensionState.PROVISIONING));
    }

    @Test
    void downscaleUpdatePassesEvenWhenOverQuota() {
        profileConfiguration.setMaxExtensionCpuMillicores(500); // quota lowered below current usage
        Extension previous = extension(ResourceSize.XLARGE, 2, ExtensionState.ACTIVE);
        Extension updated = extension(ResourceSize.XLARGE, 1, ExtensionState.ACTIVE);
        updated.setId(previous.getId());
        assertThatCode(() -> service.validateResourceCapacity(tenantId, updated, previous))
                .doesNotThrowAnyException();
        verify(extensionDao, never()).findAllByTenantId(any(), any());
    }

    /* --- Quota view --- */

    private static Long availableOf(ExtensionQuota quota, ResourceSize size) {
        return quota.availableBySize().stream()
                .filter(available -> available.size() == size)
                .findFirst().orElseThrow().count();
    }

    @Test
    void quotaReportsUsageAndFreeCapacityPerSize() {
        profileConfiguration.setMaxExtensions(5);
        profileConfiguration.setMaxExtensionCpuMillicores(2000);
        profileConfiguration.setMaxExtensionMemoryMb(1024);
        tenantHas(extension(ResourceSize.MEDIUM, 1, ExtensionState.ACTIVE)); // 1000m / 512 MB used
        when(extensionDao.countByTenantId(tenantId)).thenReturn(1L);

        ExtensionQuota quota = service.getQuota(tenantId);

        assertThat(quota.maxExtensions()).isEqualTo(5);
        assertThat(quota.usedExtensions()).isEqualTo(1);
        assertThat(quota.maxCpuMillicores()).isEqualTo(2000);
        assertThat(quota.usedCpuMillicores()).isEqualTo(1000);
        assertThat(quota.maxMemoryMb()).isEqualTo(1024);
        assertThat(quota.usedMemoryMb()).isEqualTo(512);
        // 1000m / 512 MB are free: 4 more XSMALL (250m/128Mi) fit, 2 SMALL, 1 MEDIUM, no LARGE (2000m)
        assertThat(availableOf(quota, ResourceSize.XSMALL)).isEqualTo(4);
        assertThat(availableOf(quota, ResourceSize.SMALL)).isEqualTo(2);
        assertThat(availableOf(quota, ResourceSize.MEDIUM)).isEqualTo(1);
        assertThat(availableOf(quota, ResourceSize.LARGE)).isZero();
        assertThat(availableOf(quota, ResourceSize.XXLARGE)).isZero();
    }

    @Test
    void quotaIsCappedByTheRemainingExtensionCount() {
        profileConfiguration.setMaxExtensions(2);
        profileConfiguration.setMaxExtensionCpuMillicores(100_000);
        profileConfiguration.setMaxExtensionMemoryMb(100_000);
        tenantHas(extension(ResourceSize.XSMALL, 1, ExtensionState.ACTIVE));
        when(extensionDao.countByTenantId(tenantId)).thenReturn(1L);

        ExtensionQuota quota = service.getQuota(tenantId);

        // cpu/memory would allow far more, but only one extension slot is left
        assertThat(availableOf(quota, ResourceSize.XSMALL)).isEqualTo(1);
        assertThat(availableOf(quota, ResourceSize.XXLARGE)).isEqualTo(1);
    }

    @Test
    void quotaReportsNullForAnUnlimitedDimension() {
        tenantHas(extension(ResourceSize.MEDIUM, 1, ExtensionState.ACTIVE));
        when(extensionDao.countByTenantId(tenantId)).thenReturn(1L);

        ExtensionQuota quota = service.getQuota(tenantId);

        assertThat(quota.maxCpuMillicores()).isNull();
        assertThat(quota.maxMemoryMb()).isNull();
        // the profile is the only source of the count limit, and it is unlimited here as well: every
        // dimension is open, so the free capacity per size has no number to report either
        assertThat(quota.maxExtensions()).isNull();
        assertThat(availableOf(quota, ResourceSize.XXLARGE)).isNull();
    }

    @Test
    void quotaIgnoresStoppedExtensionsForCpuAndMemory() {
        profileConfiguration.setMaxExtensionCpuMillicores(1000);
        profileConfiguration.setMaxExtensionMemoryMb(512);
        tenantHas(extension(ResourceSize.MEDIUM, 1, ExtensionState.STOPPED));
        when(extensionDao.countByTenantId(tenantId)).thenReturn(1L);

        ExtensionQuota quota = service.getQuota(tenantId);

        assertThat(quota.usedCpuMillicores()).isZero();
        assertThat(quota.usedMemoryMb()).isZero();
        // a stopped extension runs no pods, but it still occupies one of the extension slots
        assertThat(quota.usedExtensions()).isEqualTo(1);
        assertThat(availableOf(quota, ResourceSize.MEDIUM)).isEqualTo(1);
    }

    @Test
    void quotaNeverReportsNegativeFreeCapacity() {
        profileConfiguration.setMaxExtensionCpuMillicores(500); // quota lowered below current usage
        profileConfiguration.setMaxExtensionMemoryMb(128);
        tenantHas(extension(ResourceSize.LARGE, 2, ExtensionState.ACTIVE)); // 4000m / 2048 MB used
        when(extensionDao.countByTenantId(tenantId)).thenReturn(1L);

        ExtensionQuota quota = service.getQuota(tenantId);

        assertThat(quota.usedCpuMillicores()).isEqualTo(4000);
        assertThat(quota.usedMemoryMb()).isEqualTo(2048);
        assertThat(quota.availableBySize()).extracting(ExtensionSizeAvailability::count).containsOnly(0L);
    }

    // The two defaults are set independently — the profile quota in DefaultTenantProfileConfiguration, the
    // applied tier in ResourceSize.DEFAULT — and they once contradicted each other: the quota was sized for
    // one XSMALL while an omitted `resources` resolved to SMALL, so the simplest create (image + tag, no
    // resources) was rejected out of the box on a stock install. Assert the relationship rather than the
    // numbers, so raising either one without the other fails here instead of at a user's first request.
    @Test
    void defaultProfileFitsExactlyOneExtensionOfTheDefaultTier() {
        // a profile that never touched the extension limits (fresh, or stored before the fields existed)
        DefaultTenantProfileConfiguration fresh = new DefaultTenantProfileConfiguration();
        profileConfiguration.setMaxExtensions(fresh.getMaxExtensions());
        profileConfiguration.setMaxExtensionCpuMillicores(fresh.getMaxExtensionCpuMillicores());
        profileConfiguration.setMaxExtensionMemoryMb(fresh.getMaxExtensionMemoryMb());
        tenantHas();
        when(extensionDao.countByTenantId(tenantId)).thenReturn(0L);

        ExtensionQuota quota = service.getQuota(tenantId);

        assertThat(quota.maxExtensions()).isEqualTo(1);
        assertThat(availableOf(quota, ResourceSize.DEFAULT)).isEqualTo(1);
        // an extension whose resources are omitted entirely must fit too — that is the same tier
        assertThat(service.cpuLimitMillicores(extension(null, 1, null)))
                .isLessThanOrEqualTo(quota.maxCpuMillicores());
        assertThat(service.memoryLimitBytes(extension(null, 1, null)))
                .isLessThanOrEqualTo(quota.maxMemoryMb() * 1024L * 1024L);
        // and the allowance stays minimal: nothing bigger than the default tier fits
        assertThat(availableOf(quota, ResourceSize.XXLARGE)).isZero();
    }

    @Test
    void upscaleUpdateIsCheckedAgainstOtherExtensions() {
        profileConfiguration.setMaxExtensionCpuMillicores(2000);
        Extension previous = extension(ResourceSize.SMALL, 1, ExtensionState.ACTIVE); // 500m
        Extension updated = extension(ResourceSize.MEDIUM, 2, ExtensionState.ACTIVE); // 2000m
        updated.setId(previous.getId());
        tenantHas(previous, extension(ResourceSize.SMALL, 1, ExtensionState.ACTIVE)); // other: 500m
        assertThatThrownBy(() -> service.validateResourceCapacity(tenantId, updated, previous))
                .isInstanceOf(DataValidationException.class)
                .hasMessageContaining("CPU limit reached");
    }

}
