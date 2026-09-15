// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.transport.limits;

public interface TransportRateLimit {

    String getConfiguration();

    boolean tryConsume();

    boolean tryConsume(long number);

}
