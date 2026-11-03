// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.security;

/**
 * Created by ashvayka on 17.05.18.
 */
public enum ValidationResultCode {
    OK,
    UNAUTHORIZED,
    ACCESS_DENIED,
    ENTITY_NOT_FOUND,
    INTERNAL_ERROR
}
