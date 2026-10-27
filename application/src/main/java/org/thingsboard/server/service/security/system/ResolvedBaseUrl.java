// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.security.system;

/**
 * Base URL as returned by {@link SystemSecurityService#getBaseUrl}, and whether it was built from
 * the request rather than taken from the Base URL in General settings.
 */
public record ResolvedBaseUrl(String url, boolean fromRequest) {
}
