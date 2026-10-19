// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.transport.resource;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(hidden = true)
public enum ResourceType {
    LWM2M_MODEL, JKS, PKCS_12
}
