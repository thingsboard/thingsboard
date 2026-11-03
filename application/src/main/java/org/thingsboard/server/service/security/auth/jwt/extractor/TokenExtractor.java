// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security.auth.jwt.extractor;

import jakarta.servlet.http.HttpServletRequest;

public interface TokenExtractor {
    String extract(HttpServletRequest request);
}