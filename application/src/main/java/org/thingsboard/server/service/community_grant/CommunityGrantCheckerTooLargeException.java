// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import org.springframework.web.client.RestClientException;

/**
 * Thrown by {@link CommunityGrantPortalClient#downloadChecker} when the checker exceeds the configured size
 * cap, whether advertised by Content-Length or discovered mid-stream.
 */
public class CommunityGrantCheckerTooLargeException extends RestClientException {

    public CommunityGrantCheckerTooLargeException(String message) {
        super(message);
    }

}
