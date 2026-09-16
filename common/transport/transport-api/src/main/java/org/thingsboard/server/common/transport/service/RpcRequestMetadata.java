// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.service;

import lombok.Data;

import java.util.UUID;

@Data
public class RpcRequestMetadata {
    private final UUID sessionId;
    private final int requestId;
}
