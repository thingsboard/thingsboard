// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.setup;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The hand-off a fresh installation gives its operator: the portal sign-up URL to open, the claim mode that
 * URL was built with, and the token it was minted with. The URL is rendered in full and copyable whatever the
 * mode, because an installation that cannot reach the portal needs it carried by hand to a workstation that
 * can.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LicenseClaimResult {

    private String signUpUrl;
    private LicenseClaimMode mode;
    /**
     * The token this claim was minted with, so the client can name it when it polls and be told that a later
     * request has superseded it. Carried as its own field rather than left to be parsed back out of
     * {@link #signUpUrl}, which already discloses it.
     */
    private String claimToken;

}
