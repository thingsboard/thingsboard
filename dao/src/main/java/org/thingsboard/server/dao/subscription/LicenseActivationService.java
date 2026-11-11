// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.subscription;

import org.thingsboard.license.client.AbstractTbLicenseClient;
import org.thingsboard.server.common.data.subscription.SubscriptionException;
import org.thingsboard.server.common.data.subscription.SubscriptionInfo;

/**
 * Owns this node's licence client for its whole life: the startup activation, the key a system administrator
 * applies, the cluster reconvergence, the scheduled retry, the clear, and the release of a displaced licence's
 * instance.
 * <p>
 * Split out of {@link BasicSubscriptionService}, which keeps entitlement enforcement, reporting and
 * non-production. The line between them is mutation: every field guarded by the activation lock or by this
 * service's monitor is here, and the rest only ever reads the current client through {@link #getClient()}.
 */
public interface LicenseActivationService {

    /**
     * The client this node is currently running on, or null when there is none. A read of a field that
     * changes under this service's monitor, so a caller must use the instance it is handed rather than
     * re-reading: it can be nulled at any moment.
     */
    AbstractTbLicenseClient getClient();

    /**
     * Whether the licence this node runs on is an offline one. Mirrored alongside the client rather than
     * recomputed from it, so no licence field can outlive the client it describes.
     */
    boolean isOfflineLicense();

    /**
     * Whether this node holds a licence, and therefore whether its management plane is unlocked.
     * <p>
     * A single volatile read on an activated node; on a node that is not activated, or is activated only off
     * the keyless client, it also attempts to converge on a licence secret another node stored - taking a
     * single-flight lock and possibly one synchronous call to the licence server. That attempt is throttled,
     * but treat this as a request-scoped check rather than something free to call in a loop.
     */
    boolean isLicenseActivated();

    int getLicenseVersion();

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
     * What {@code secret} would install, without installing it. An offline blob is read locally; a short key
     * is checked with the licence portal through an endpoint that writes nothing. The licence it describes,
     * with this deployment's live counts beside it, so the two can be compared row by row.
     * <p>
     * A key that covers fewer devices or assets than this deployment already holds is refused here too, so the
     * dialog says no rather than previewing clean and failing seconds later.
     *
     * @throws SubscriptionException carrying an operator-facing message from {@link LicenseKeyRejections}, or
     * from {@link LicenseCapacity} when the live fleet does not fit
     */
    SubscriptionInfo previewLicenseKey(String secret);

    /** Removes the licence from the whole deployment, so the activation flow can be run again. */
    void clearLicense();

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
     * Attempts, bypassing the throttle that guards the ordinary on-demand attempt, to activate this node from
     * a licence secret another node already stored. A convergence step, not a source of truth: it can
     * legitimately fail on a cluster that is genuinely activated, so a caller that needs to know the cluster's
     * state must read the stored secret instead. Only for callers whose immediately following request is
     * answered from this node's own activation state.
     */
    void pickUpStoredLicenseSecret();

}
