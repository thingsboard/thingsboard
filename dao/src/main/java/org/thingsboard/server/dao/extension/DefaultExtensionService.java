// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.extension;

import com.google.common.util.concurrent.FluentFuture;
import io.fabric8.kubernetes.api.model.Quantity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.extension.Extension;
import org.thingsboard.server.common.data.extension.ExtensionInfo;
import org.thingsboard.server.common.data.extension.ExtensionMode;
import org.thingsboard.server.common.data.extension.ExtensionQuota;
import org.thingsboard.server.common.data.extension.ExtensionSizeAvailability;
import org.thingsboard.server.common.data.extension.ExtensionState;
import org.thingsboard.server.common.data.extension.ResourceSize;
import org.thingsboard.server.common.data.extension.ResourceSpec;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.ExtensionId;
import org.thingsboard.server.common.data.id.HasId;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.page.PageData;
import org.thingsboard.server.common.data.page.PageLink;
import org.thingsboard.server.common.data.tenant.profile.DefaultTenantProfileConfiguration;
import org.thingsboard.server.dao.DaoUtil;
import org.thingsboard.server.dao.entity.AbstractEntityService;
import org.thingsboard.server.dao.eventsourcing.DeleteEntityEvent;
import org.thingsboard.server.dao.eventsourcing.SaveEntityEvent;
import org.thingsboard.server.dao.model.sql.ExtensionEntity;
import org.thingsboard.server.dao.service.DataValidator;
import org.thingsboard.server.dao.sql.JpaExecutorService;
import org.thingsboard.server.dao.usagerecord.ApiLimitService;
import org.thingsboard.server.exception.DataValidationException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.thingsboard.server.dao.service.Validator.validatePageLink;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultExtensionService extends AbstractEntityService implements ExtensionService {

    static final int MAX_SLUG_ATTEMPTS = 8;

    // The tenant-profile memory quota is entered in megabytes (MiB); internally everything is bytes.
    static final long BYTES_IN_MB = 1024L * 1024L;

    static final String SLUG_CONSTRAINT = "extension_slug_unq_key";
    static final String NAME_CONSTRAINT = "extension_tenant_name_unq_key";
    static final String EXTERNAL_ID_CONSTRAINT = "extension_external_id_unq_key";

    private final ExtensionDao extensionDao;
    private final DataValidator<Extension> extensionValidator;
    private final JpaExecutorService jpaExecutor;
    private final ApiLimitService apiLimitService;
    private final ExtensionQuotaGuard quotaGuard;

    // Infrastructure mode drives whether the slug carries a random suffix; the slug toggle overrides it.
    @Value("${extensions.mode:disabled}")
    private String extensionsMode = "disabled";
    // Empty = derive from mode (suffix only in public-cloud). Set true/false to force it.
    @Value("${extensions.slug.enforce-suffix:}")
    private String enforceSuffixOverride = "";
    // Resolved once from enforceSuffixOverride: TRUE/FALSE to force, null to use the mode default.
    private Boolean enforceSuffixResolved;

    @PostConstruct
    void resolveEnforceSuffixOverride() {
        if (StringUtils.isBlank(enforceSuffixOverride)) {
            enforceSuffixResolved = null;
            return;
        }
        String value = enforceSuffixOverride.trim();
        if (value.equalsIgnoreCase("true")) {
            enforceSuffixResolved = Boolean.TRUE;
        } else if (value.equalsIgnoreCase("false")) {
            enforceSuffixResolved = Boolean.FALSE;
        } else {
            // Do NOT silently treat an unrecognized value as false (Boolean.parseBoolean would): in
            // public-cloud that drops the random slug suffix — a security control. Ignore the malformed
            // override and fall back to the mode default instead.
            enforceSuffixResolved = null;
            log.warn("Ignoring unrecognized extensions.slug.enforce-suffix='{}' (expected 'true' or 'false'); "
                    + "using the default for extensions.mode={}.", enforceSuffixOverride, extensionsMode);
        }
    }

    // Not @Transactional: each saveAndFlush runs in its own tx so a slug collision rolls back only that attempt.
    @Override
    public Extension saveExtension(Extension extension) {
        boolean isCreate = extension.getId() == null;
        Extension old = extensionValidator.validate(extension, Extension::getTenantId);
        if (isCreate) {
            extension.setState(ExtensionState.PROVISIONING);
            extension.setLastError(null);
            extension.setLastErrorAt(null);
            return saveWithAllocatedSlug(extension);
        }
        extension.setSlug(old.getSlug());
        extension.setCreatedTime(old.getCreatedTime());
        extension.setExternalId(old.getExternalId()); // immutable after create; ignore client-supplied changes
        try {
            return saveAndPublish(extension, old, () -> {
                // `old` predates the guard and optimistic locking won't catch a concurrent transition:
                // base the preserved state/lastError and the quota-skip decision on the current row.
                Extension current = extensionDao.findByTenantIdAndId(extension.getTenantId(), extension.getId());
                if (current == null) {
                    throw new DataValidationException("Extension was deleted!");
                }
                extension.setState(current.getState());
                extension.setLastError(current.getLastError());
                extension.setLastErrorAt(current.getLastErrorAt());
                if (current.getState() != ExtensionState.STOPPED) {
                    // A STOPPED extension runs no pods and occupies no quota; capacity is re-checked on start.
                    validateResourceCapacity(extension.getTenantId(), extension, current);
                }
            });
        } catch (RuntimeException e) {
            log.error("Exception on save extension slug={} error={}", extension.getSlug(), e.getMessage());
            mapNameAndExternalIdConstraints(e);
            throw e;
        }
    }

    private void createQuotaChecks(Extension extension) {
        validateExtensionCount(extension.getTenantId());
        validateResourceCapacity(extension.getTenantId(), extension, null);
    }

    private Extension saveWithAllocatedSlug(Extension extension) {
        String base = StringUtils.isNotBlank(extension.getSlugBase())
                ? ExtensionSlugUtils.normalizeBase(extension.getSlugBase())
                : ExtensionSlugUtils.slugify(extension.getName());
        extension.setSlugBase(null);
        if (ExtensionSlugUtils.isReserved(base)) {
            // A slug that renders to an operator-owned object name (e.g. tb-ext-router) would let the
            // reconciler overwrite/delete shared infrastructure; reject it before any slug is assigned.
            throw new DataValidationException("Extension slug '" + base + "' is reserved by the platform!");
        }
        if (!enforceSlugSuffix()) {
            // private-cloud / docker-compose: the slug is exactly the base (still system-wide unique).
            return saveWithExactSlug(extension, base);
        }
        for (int attempt = 0; attempt < MAX_SLUG_ATTEMPTS; attempt++) {
            String candidate = base + "-" + ExtensionSlugUtils.random4Hex();
            if (extensionDao.findBySlug(candidate) != null) {
                continue;
            }
            extension.setSlug(candidate);
            try {
                return saveAndPublish(extension, null, () -> createQuotaChecks(extension));
            } catch (RuntimeException e) {
                // @SqlDao is not @Repository, so inspect the raw Hibernate constraint name.
                String constraint = DaoUtil.extractConstraintViolationException(e)
                        .map(ConstraintViolationException::getConstraintName).orElse(null);
                if (SLUG_CONSTRAINT.equals(constraint)) {
                    log.warn("Conflict of the slug on save extension slug={} error={}", extension.getSlug(), e.getMessage());
                    continue; // check-then-insert race between cluster nodes
                }
                mapNameAndExternalIdConstraints(e);
                throw e;
            }
        }
        throw new DataValidationException("Could not allocate a unique slug for base '" + base + "'.");
    }

    // No-suffix modes: the user-chosen base IS the slug. Still system-wide unique (it is the routing
    // key), so a collision surfaces as a clean error rather than silently appending a suffix.
    private Extension saveWithExactSlug(Extension extension, String base) {
        extension.setSlug(base);
        try {
            return saveAndPublish(extension, null, () -> createQuotaChecks(extension));
        } catch (RuntimeException e) {
            checkConstraintViolation(e, SLUG_CONSTRAINT, "Extension slug '" + base + "' is already in use!");
            mapNameAndExternalIdConstraints(e);
            throw e;
        }
    }

    // Suffix is forced only in public cloud (many untrusted tenants share one slug space); the
    // single-tenant / trusted modes keep the exact slug. extensions.slug.enforce-suffix overrides.
    private boolean enforceSlugSuffix() {
        if (enforceSuffixResolved != null) {
            return enforceSuffixResolved;
        }
        return ExtensionMode.fromKey(extensionsMode).enforcesSlugSuffix();
    }

    private void mapNameAndExternalIdConstraints(RuntimeException e) {
        checkConstraintViolation(e,
                NAME_CONSTRAINT, "Extension with such name already exists!",
                EXTERNAL_ID_CONSTRAINT, "Extension with such external id already exists!");
    }

    /**
     * Runs the quota checks and the write as one guarded unit, then publishes.
     *
     * <p>The checks CANNOT be hoisted out of the guard: they are aggregates over the tenant's rows, so a
     * concurrent request slipping between a hoisted check and this write is exactly the overrun the guard
     * exists to prevent. The event is published only after the transaction commits, so a write that rolled
     * back never triggers a reconcile.
     */
    private Extension saveAndPublish(Extension extension, Extension old, Runnable quotaChecks) {
        Extension saved = quotaGuard.run(extension.getTenantId(), () -> {
            quotaChecks.run();
            return extensionDao.saveAndFlush(extension.getTenantId(), extension);
        });
        eventPublisher.publishEvent(SaveEntityEvent.builder()
                .tenantId(saved.getTenantId())
                .entityId(saved.getId())
                .entity(saved)
                .oldEntity(old)
                .created(old == null)
                .broadcastEvent(true)
                .build());
        return saved;
    }

    @Override
    public Extension findExtensionById(TenantId tenantId, ExtensionId extensionId) {
        return extensionDao.findByTenantIdAndId(tenantId, extensionId);
    }

    // Chunked so a node holding thousands of extensions never builds one enormous IN list: PostgreSQL
    // handles it, but the bound-parameter count is what kills the prepared-statement plan cache.
    private static final int FIND_BY_IDS_CHUNK = 512;

    @Override
    public List<Extension> findExtensionsByIds(Collection<ExtensionId> extensionIds) {
        if (extensionIds == null || extensionIds.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = extensionIds.stream().filter(Objects::nonNull).map(ExtensionId::getId).toList();
        List<Extension> result = new ArrayList<>(ids.size());
        for (int from = 0; from < ids.size(); from += FIND_BY_IDS_CHUNK) {
            result.addAll(extensionDao.findByIds(ids.subList(from, Math.min(from + FIND_BY_IDS_CHUNK, ids.size()))));
        }
        return result;
    }

    @Override
    public Extension findBySlug(String slug) {
        if (StringUtils.isBlank(slug)) {
            return null;
        }
        return extensionDao.findBySlug(slug);
    }

    @Override
    public Extension findByTenantIdAndName(TenantId tenantId, String name) {
        if (tenantId == null || StringUtils.isBlank(name)) {
            return null;
        }
        return extensionDao.findByTenantIdAndName(tenantId.getId(), name);
    }

    @Override
    public PageData<ExtensionInfo> findExtensionInfosByTenantId(TenantId tenantId, PageLink pageLink) {
        validatePageLink(pageLink, ExtensionEntity.ALLOWED_SORT_PROPERTIES);
        return extensionDao.findInfosByTenantId(tenantId.getId(), pageLink);
    }

    @Override
    public PageData<Extension> findAllExtensions(PageLink pageLink) {
        return extensionDao.findAll(pageLink);
    }

    @Override
    @Transactional
    public void deleteExtension(TenantId tenantId, ExtensionId extensionId) {
        Extension extension = extensionDao.findByTenantIdAndId(tenantId, extensionId);
        if (extension == null) {
            return;
        }
        if (extensionDao.deleteByTenantIdAndId(tenantId, extensionId)) {
            eventPublisher.publishEvent(DeleteEntityEvent.builder()
                    .tenantId(tenantId).entityId(extensionId).entity(extension).build());
        }
    }

    @Override
    @Transactional
    public void deleteExtensionsByTenantId(TenantId tenantId) {
        List<Extension> extensions = new ArrayList<>();
        PageLink pageLink = new PageLink(100);
        PageData<Extension> page;
        do {
            page = extensionDao.findAllByTenantId(tenantId, pageLink);
            extensions.addAll(page.getData());
            pageLink = pageLink.nextPageLink();
        } while (page.hasNext());
        if (extensions.isEmpty()) {
            return;
        }
        extensionDao.deleteByTenantIdAndIds(tenantId,
                extensions.stream().map(extension -> extension.getId().getId()).collect(Collectors.toSet()));
        // The reconcile teardown listens for per-extension delete events, so the bulk path
        // still publishes one event per deleted extension.
        extensions.forEach(extension -> eventPublisher.publishEvent(DeleteEntityEvent.builder()
                .tenantId(tenantId).entityId(extension.getId()).entity(extension).build()));
    }

    @Override
    @Transactional
    public void updateState(TenantId tenantId, ExtensionId extensionId, ExtensionState state, String lastError) {
        Extension extension = extensionDao.findByTenantIdAndId(tenantId, extensionId);
        if (extension == null) {
            return;
        }
        extension.setState(state);
        extension.setLastError(lastError);
        extension.setLastErrorAt(lastError != null ? System.currentTimeMillis() : null);
        extensionDao.save(tenantId, extension);
    }

    @Override
    public void startWithinQuota(TenantId tenantId, Extension extension) {
        quotaGuard.run(tenantId, () -> {
            // Re-read inside the guard: the caller's snapshot is stale, and the update path skips the
            // capacity check for STOPPED rows — a concurrent save + start would bypass the quota.
            Extension fresh = extensionDao.findByTenantIdAndId(tenantId, extension.getId());
            if (fresh == null) {
                throw new DataValidationException("Extension was deleted!");
            }
            if (fresh.getState() != ExtensionState.STOPPED) {
                throw new DataValidationException("Extension can only be started from the STOPPED state (current: "
                        + fresh.getState() + ").");
            }
            validateResourceCapacity(tenantId, fresh, null);
            updateState(tenantId, fresh.getId(), ExtensionState.PROVISIONING, null);
            return null;
        });
    }

    @Override
    public boolean updateStateIf(TenantId tenantId, ExtensionId extensionId, ExtensionState expectedState,
                                 ExtensionState newState, String lastError) {
        // One conditional statement: the check and the write cannot be split by a concurrent commit,
        // so no lock and no surrounding transaction are needed.
        Long lastErrorAt = lastError != null ? System.currentTimeMillis() : null;
        boolean applied = extensionDao.updateStateIf(tenantId, extensionId, expectedState, newState, lastError, lastErrorAt);
        if (!applied) {
            log.debug("[{}][{}] State update {} -> {} skipped: the row is no longer {}",
                    tenantId, extensionId, expectedState, newState, expectedState);
        }
        return applied;
    }

    @Override
    public Optional<HasId<?>> findEntity(TenantId tenantId, EntityId entityId) {
        return Optional.ofNullable(findExtensionById(tenantId, new ExtensionId(entityId.getId())));
    }

    @Override
    public FluentFuture<Optional<HasId<?>>> findEntityAsync(TenantId tenantId, EntityId entityId) {
        return FluentFuture.from(jpaExecutor.submit(() ->
                Optional.ofNullable((HasId<?>) findExtensionById(tenantId, new ExtensionId(entityId.getId())))));
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return extensionDao.countByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deleteEntity(TenantId tenantId, EntityId id, boolean force) {
        deleteExtension(tenantId, new ExtensionId(id.getId()));
    }

    @Override
    @Transactional
    public void deleteByTenantId(TenantId tenantId) {
        deleteExtensionsByTenantId(tenantId);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.EXTENSION;
    }

    // The tenant profile is the single source of the count limit (0 = unlimited). It defaults to one
    // extension of the default tier, so an install that never touches the profile is still bounded.
    private long maxExtensions(TenantId tenantId) {
        return Math.max(0, apiLimitService.getLimit(tenantId, DefaultTenantProfileConfiguration::getMaxExtensions));
    }

    private void validateExtensionCount(TenantId tenantId) {
        long max = maxExtensions(tenantId);
        if (max > 0 && extensionDao.countByTenantId(tenantId) >= max) {
            throw new DataValidationException("Extension limit reached for this tenant (max " + max + ")!");
        }
    }

    // Not exposed on ExtensionService: a bare capacity check invites the check-then-act it cannot survive.
    // Callers go through saveExtension / startWithinQuota, which run it inside the per-tenant guard.
    void validateResourceCapacity(TenantId tenantId, Extension extension) {
        validateResourceCapacity(tenantId, extension, null);
    }

    /**
     * Enforces the tenant-profile aggregate CPU/memory quota for extension deployments. The occupied
     * amount is the sum of pod limits multiplied by replicas over every extension of the tenant that is
     * not STOPPED, excluding the candidate itself.
     *
     * <p>Deleted extensions stop counting the moment their row is removed, which is before teardown
     * finishes, so a delete-then-create sequence can transiently exceed the quota for as long as the old
     * pods take to terminate. There is no DELETING grace period: {@link ExtensionState#DELETING} is
     * defined but never written — the delete is a hard row delete plus a post-commit teardown event.
     * With {@code previous} set (an update), a dimension is only checked when the new demand exceeds the
     * old one — a downscale must always pass, even when the quota was lowered below current usage.
     */
    void validateResourceCapacity(TenantId tenantId, Extension candidate, Extension previous) {
        long maxCpuMillicores = apiLimitService.getLimit(tenantId,
                DefaultTenantProfileConfiguration::getMaxExtensionCpuMillicores);
        long maxMemoryBytes = apiLimitService.getLimit(tenantId,
                DefaultTenantProfileConfiguration::getMaxExtensionMemoryMb) * BYTES_IN_MB;
        long neededCpu = cpuLimitMillicores(candidate);
        long neededMemory = memoryLimitBytes(candidate);
        boolean checkCpu = maxCpuMillicores > 0 && (previous == null || neededCpu > cpuLimitMillicores(previous));
        boolean checkMemory = maxMemoryBytes > 0 && (previous == null || neededMemory > memoryLimitBytes(previous));
        if (!checkCpu && !checkMemory) {
            return;
        }
        ResourceUsage used = currentUsage(tenantId, candidate.getId());
        if (checkCpu && used.cpuMillicores() + neededCpu > maxCpuMillicores) {
            throw new DataValidationException("Extensions CPU limit reached for this tenant: requested "
                    + neededCpu + "m, used " + used.cpuMillicores() + "m of " + maxCpuMillicores + "m!");
        }
        if (checkMemory && used.memoryBytes() + neededMemory > maxMemoryBytes) {
            throw new DataValidationException("Extensions memory limit reached for this tenant: requested "
                    + (neededMemory / BYTES_IN_MB) + " MB, used " + (used.memoryBytes() / BYTES_IN_MB) + " MB of "
                    + (maxMemoryBytes / BYTES_IN_MB) + " MB!");
        }
    }

    @Override
    public ExtensionQuota getQuota(TenantId tenantId) {
        long maxExtensions = maxExtensions(tenantId);
        long maxCpuMillicores = apiLimitService.getLimit(tenantId,
                DefaultTenantProfileConfiguration::getMaxExtensionCpuMillicores);
        long maxMemoryMb = apiLimitService.getLimit(tenantId,
                DefaultTenantProfileConfiguration::getMaxExtensionMemoryMb);
        long usedExtensions = extensionDao.countByTenantId(tenantId);
        ResourceUsage used = currentUsage(tenantId, null);
        long freeExtensions = Math.max(0, maxExtensions - usedExtensions);
        long freeCpu = Math.max(0, maxCpuMillicores - used.cpuMillicores());
        long freeMemoryBytes = Math.max(0, maxMemoryMb * BYTES_IN_MB - used.memoryBytes());
        List<ExtensionSizeAvailability> availableBySize = Arrays.stream(ResourceSize.values())
                .map(size -> new ExtensionSizeAvailability(size, availableCount(size,
                        maxExtensions > 0 ? freeExtensions : null,
                        maxCpuMillicores > 0 ? freeCpu : null,
                        maxMemoryMb > 0 ? freeMemoryBytes : null)))
                .toList();
        return new ExtensionQuota(
                maxExtensions > 0 ? maxExtensions : null, usedExtensions,
                maxCpuMillicores > 0 ? maxCpuMillicores : null, used.cpuMillicores(),
                maxMemoryMb > 0 ? maxMemoryMb : null, used.memoryBytes() / BYTES_IN_MB,
                availableBySize);
    }

    /**
     * How many more single-replica extensions of the tier fit: the smallest of what the still free
     * extension count, CPU and memory allow. A null free value means that limit is unlimited; when every
     * limit is unlimited the result is null as well.
     */
    private static Long availableCount(ResourceSize size, Long freeExtensions, Long freeCpuMillicores, Long freeMemoryBytes) {
        Long count = freeExtensions;
        if (freeCpuMillicores != null) {
            long fits = freeCpuMillicores / cpuLimitMillicores(size);
            count = count != null ? Math.min(count, fits) : fits;
        }
        if (freeMemoryBytes != null) {
            long fits = freeMemoryBytes / memoryLimitBytes(size);
            count = count != null ? Math.min(count, fits) : fits;
        }
        return count;
    }

    /** CPU/memory occupied by the tenant's extensions; STOPPED ones run no pods and are skipped. */
    private ResourceUsage currentUsage(TenantId tenantId, ExtensionId excludedId) {
        long usedCpu = 0;
        long usedMemory = 0;
        PageLink pageLink = new PageLink(100);
        PageData<Extension> page;
        do {
            page = extensionDao.findAllByTenantId(tenantId, pageLink);
            for (Extension extension : page.getData()) {
                if (extension.getState() == ExtensionState.STOPPED
                        || (excludedId != null && excludedId.equals(extension.getId()))) {
                    continue;
                }
                usedCpu += cpuLimitMillicores(extension);
                usedMemory += memoryLimitBytes(extension);
            }
            pageLink = pageLink.nextPageLink();
        } while (page.hasNext());
        return new ResourceUsage(usedCpu, usedMemory);
    }

    private record ResourceUsage(long cpuMillicores, long memoryBytes) {}

    // Total CPU demand of the extension's pods (cpu limit x scheduled replicas), in millicores.
    long cpuLimitMillicores(Extension extension) {
        return millicores(effectiveResources(extension).effectiveCpuLimit()) * scheduledReplicas(extension);
    }

    // Total memory demand of the extension's pods (memory limit x scheduled replicas), in bytes.
    long memoryLimitBytes(Extension extension) {
        return quantityValue(effectiveResources(extension).effectiveMemoryLimit()).longValue() * scheduledReplicas(extension);
    }

    // What the active substrate will really run, which is not always what the spec asks for: the Docker
    // backend has no load balancer behind one container name, so it starts exactly one container however
    // many replicas are requested. Charging quota for the requested number there would reject deploys
    // while the capacity sits physically idle and report usage no running container accounts for.
    private int scheduledReplicas(Extension extension) {
        int requested = replicasOf(extension);
        return ExtensionMode.fromKey(extensionsMode).runsRequestedReplicas() ? requested : Math.min(requested, 1);
    }

    // CPU demand of a single pod of the tier, in millicores.
    static long cpuLimitMillicores(ResourceSize size) {
        return millicores(size.getCpuLimit());
    }

    // Memory demand of a single pod of the tier, in bytes.
    static long memoryLimitBytes(ResourceSize size) {
        return quantityValue(size.getMemoryLimit()).longValue();
    }

    private static long millicores(String cpuQuantity) {
        return quantityValue(cpuQuantity).multiply(BigDecimal.valueOf(1000)).longValue();
    }

    private static ResourceSpec effectiveResources(Extension extension) {
        ResourceSpec resources = extension.getContainerSpec() != null
                ? extension.getContainerSpec().getResources() : null;
        return resources != null ? resources : new ResourceSpec();
    }

    private static int replicasOf(Extension extension) {
        return extension.getContainerSpec() != null ? Math.max(1, extension.getContainerSpec().getReplicas()) : 1;
    }

    // Kubernetes quantity to a base value: CPU in cores, memory in bytes.
    private static BigDecimal quantityValue(String raw) {
        return Quantity.getAmountInBytes(new Quantity(raw.trim()));
    }

}
