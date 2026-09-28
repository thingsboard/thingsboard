// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.thingsboard.server.common.data.LicenseInfo;
import org.thingsboard.server.common.data.id.TenantId;
import org.thingsboard.server.common.data.subscription.SubscriptionErrorCode;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;

public interface SubscriptionService {

    void createDeviceAllowed(TenantId tenantId) throws SubscriptionException;

    void createAssetAllowed(TenantId tenantId) throws SubscriptionException;

    void createEdgeAllowed(TenantId tenantId) throws SubscriptionException;

    boolean isCreateEdgeAllowed(TenantId tenantId);

    void createAgentAllowed(TenantId tenantId) throws SubscriptionException;

    void whiteLabelingAllowed(TenantId tenantId) throws SubscriptionException;

    boolean whiteLabelingEnabled(TenantId tenantId) throws SubscriptionException;

    boolean edgeEnabled(TenantId tenantId) throws SubscriptionException;

    boolean trendzEnabled(TenantId tenantId) throws SubscriptionException;

    /**
     * Whether the licence grants this platform feature; a licence that says nothing about it grants it, see
     * {@link PlatformFeature}. Deployment-wide: {@code tenantId} is not consulted.
     */
    boolean isFeatureEnabled(TenantId tenantId, PlatformFeature feature);

    /** Throws unless {@link #isFeatureEnabled(TenantId, PlatformFeature)} grants the feature. */
    void checkFeatureAllowed(TenantId tenantId, PlatformFeature feature) throws SubscriptionException;

    /** Whether this deployment runs in development (non-production) mode; {@code tenantId} is not consulted. */
    boolean isDevelopment(TenantId tenantId) throws SubscriptionException;

    boolean solutionTemplateLevelAllowed(TenantId tenantId, String solutionTemplateLevel) throws SubscriptionException;

    LicenseInfo getLicenseInfo();

    int getLicenseVersion();

    SubscriptionInfo getSubscriptionInfo();

    SubscriptionInfo refreshLicense();

    /**
     * Whether this node holds a licence, and therefore whether its management plane is unlocked.
     * <p>
     * A single volatile read on an activated node; on a node that is not activated, or is activated only off
     * the keyless client, it also attempts to converge on a licence secret another node stored - taking a
     * single-flight lock and possibly one synchronous call to the licence server. That attempt is throttled,
     * but treat this as a request-scoped check rather than something free to call in a loop.
     */
    boolean isLicenseActivated();

    /**
     * Attempts, bypassing the throttle that guards the ordinary on-demand attempt, to activate this node from
     * a licence secret another node already stored. A convergence step, not a source of truth: it can
     * legitimately fail on a cluster that is genuinely activated, so a caller that needs to know the cluster's
     * state must read the stored secret instead. Only for callers whose immediately following request is
     * answered from this node's own activation state.
     */
    void pickUpStoredLicenseSecret();

    /**
     * Re-reads the cluster's licence state from {@code tb_cluster} and brings this node onto it, whichever way
     * it changed: a secret replaced, a secret removed, or a locked or keyless node picking up one the cluster
     * now holds. Bypasses the per-secret and per-node throttles, which exist to cap the unauthenticated request
     * path and would otherwise delay an operator-initiated change by up to five minutes.
     * <p>
     * A reader only. It never writes {@code tb_cluster} and never consults the environment secret: the node
     * that made the change is the writer, and resolving the environment here would let a peer answer a cleared
     * licence by re-seeding it for the whole cluster.
     */
    void reconcileLicenseState();

    /**
     * Whether the instance is running keyless, off a synthesized licence for non-production use, rather than
     * off a real licence secret.
     */
    boolean isNonProductionMode();

    /**
     * Locks the instance because its non-production entitlement was exhausted while it was already running,
     * not only at startup. Idempotent, and re-checks {@link #isNonProductionMode()} itself.
     *
     * @return whether the entitlement was actually revoked. False means the node holds a real licence by
     * now, so the caller must not act on the revocation either.
     */
    boolean revokeNonProductionEntitlement();

    void applyLicenseKey(String secret);

    /**
     * What {@code secret} would install, without installing it: nothing is activated, no instance slot is
     * spent and the deployment keeps the licence it has. See
     * {@link LicenseActivationService#previewLicenseKey(String)}.
     *
     * @throws SubscriptionException carrying an operator-facing message when the key is refused
     */
    default SubscriptionInfo previewLicenseKey(String secret) {
        // Licence enforcement is disabled under the install/test profiles, so there is no licence to compare against.
        throw new SubscriptionException("License preview is not available on this instance",
                SubscriptionErrorCode.FEATURE_DISABLED);
    }

    /** Removes the licence from the whole deployment, so the activation flow can be run again. */
    default void clearLicense() {
        // no-op: license enforcement is disabled under install/test profiles
    }

    default String getAiToken() {
        return null;
    }

    /**
     * Whether the licence this instance runs on is one the additional device- and asset-count enforcement applies to.
     * False whenever there is no answer, so losing the licence can never turn the enforcement on.
     */
    default boolean isCommunityGrantLicense() {
        return false;
    }

    /** The device quota this instance is licensed for, or {@code 0} when it is unlimited or unknown. */
    default long getLicensedDeviceLimit() {
        return 0;
    }

    /** The asset quota this instance is licensed for, or {@code 0} when it is unlimited or unknown. */
    default long getLicensedAssetLimit() {
        return 0;
    }

}
