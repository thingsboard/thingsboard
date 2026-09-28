// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trendz;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.thingsboard.server.common.data.exception.ThingsboardException;

public interface TrendzProxyService {
    ResponseEntity<byte[]> proxy(HttpServletRequest request, byte[] body) throws ThingsboardException;
}
