// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

/**
 * Thrown by {@link CommunityGrantPortalClient#claimLicense} when the portal will not hand the license key
 * over. Terminal, unlike a transport failure: retrying the claim cannot change the answer.
 */
public class CommunityGrantLicenseClaimRefusedException extends RestClientException {

    public CommunityGrantLicenseClaimRefusedException(HttpStatusCodeException cause) {
        super(cause.getMessage(), cause);
    }

}
