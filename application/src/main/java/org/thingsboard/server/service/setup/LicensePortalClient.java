// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.setup;

import org.thingsboard.license.shared.FreeLicenseClaimResponse;
import org.thingsboard.license.shared.exception.LicenseException;

/**
 * The outbound license portal calls of the setup flow, behind an injectable interface: the underlying client
 * methods are static, which would otherwise make the error handling around them untestable without static
 * mocking.
 */
public interface LicensePortalClient {

    /**
     * Whether the portal is reachable from this host. Asked before an operator is handed a sign-up URL, so that
     * an installation with no outbound access is offered the offline route.
     * <p>
     * Two properties any implementation owes: it never throws - every failure to reach the portal is a
     * {@code false} - and an HTTP error status is {@code true}, since the portal answered.
     */
    boolean isPortalReachable();

    /** The portal this instance will claim from - the same base the sign-up URL is built on. */
    String getPortalBaseUrl();

    /**
     * Asks the portal whether the free license this token was minted for has been activated yet, and
     * retrieves its secret if so.
     */
    FreeLicenseClaimResponse claimFreeLicense(String claimToken) throws LicenseException;

}
