// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.community_grant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The portal's answer to a poll. {@code status} is a raw string so an unknown value leaves the flow where it
 * is rather than failing deserialization.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CommunityGrantPortalStatus(String status,
                                         JsonNode checkerInput,
                                         Integer retryAfterSec) {
}
