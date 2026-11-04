// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.setup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.thingsboard.license.client.TbLicenseClient;
import org.thingsboard.license.shared.FreeLicenseClaimResponse;
import org.thingsboard.license.shared.exception.LicenseException;

/**
 * Delegates to the static license client calls. A null endpoint makes the client resolve it the same way
 * as for any other license call: the "tb.license.server" system property, then the public portal.
 */
@Component
@Slf4j
public class DefaultLicensePortalClient implements LicensePortalClient {

    /**
     * Guarded here rather than left to the delegate: {@link LicensePortalClient#isPortalReachable()} promises
     * its callers that it never throws, and anything escaping this call would surface to the operator as a
     * failure to build a sign-up link, which is not what an unreachable portal means.
     */
    @Override
    public boolean isPortalReachable() {
        try {
            return TbLicenseClient.isPortalReachable(null);
        } catch (Exception e) {
            log.debug("The license portal reachability probe failed", e);
            return false;
        }
    }

    @Override
    public String getPortalBaseUrl() {
        return TbLicenseClient.resolvePortalBaseUrl(null);
    }

    @Override
    public FreeLicenseClaimResponse claimFreeLicense(String claimToken) throws LicenseException {
        return TbLicenseClient.claimFreeLicense(null, claimToken);
    }

}
