// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.mqtt;

@FunctionalInterface
public interface ReconnectStrategy {
    long getNextReconnectDelay();
}
