// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.setup;

/**
 * Whether this instance has an automatic licence claim outstanding. Only {@link #ACTIVATED} is about
 * activation at all; the rest describe the claim. Kept separate from {@link SystemSetupState}, which owns the
 * setup state, so the two cannot disagree.
 */
public enum LicenseClaimStatus {

    /**
     * No claim is outstanding on an instance that still needs a license: it never requested one, or its claim
     * was retired. The wizard only polls after a claim request, so it renders this as the same dead end as
     * {@link #EXPIRED} - request a claim again.
     */
    NOT_REQUESTED,
    /** The portal holds the claim token and the user has not completed the activation yet. Keep polling. */
    PENDING,
    /**
     * The caller's claim is dead and polling it again cannot change that. Either the portal refused it
     * outright and will refuse it again - the token's window has closed, the claim was already spent, or the
     * account behind it can no longer resolve a licence for this installation - or a later claim request has
     * superseded the token the poll named, which is what a second browser session starting the activation
     * does to the first. Terminal, and distinct from a generic error so the UI can say "your sign-up link
     * expired, start again" - the honest reading of all of them from here, since the portal deliberately
     * answers every refusal identically rather than telling an unauthenticated caller which it was.
     * <p>
     * A refusal clears the token, so the next poll answers {@link #NOT_REQUESTED} instead. A superseded token
     * keeps answering this, since the claim that replaced it is alive and only its own holder may retire it.
     * A poll that names no token is never superseded: it reports on whatever claim is stored.
     */
    EXPIRED,
    /**
     * The license is in place: this poll either claimed and applied the secret, or found the instance already
     * activated by some other route. Re-read the setup state once and move on.
     */
    ACTIVATED

}
