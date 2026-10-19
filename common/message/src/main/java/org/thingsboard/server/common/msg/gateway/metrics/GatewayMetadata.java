// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.msg.gateway.metrics;

public record GatewayMetadata(String connector, long receivedTs, long publishedTs) {
}
