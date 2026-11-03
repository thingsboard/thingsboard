// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.exception;

public class ThingsboardKafkaClientError extends Error {

    public ThingsboardKafkaClientError(String message) {
        super(message);
    }
}
