// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data;

import lombok.Getter;

@Getter
public class ServiceFailureException extends RuntimeException {

    private final Object serviceKey;

    public ServiceFailureException(Object serviceKey, Throwable cause) {
        super(cause.getMessage(), cause);
        this.serviceKey = serviceKey;
    }

    public ServiceFailureException(Object serviceKey, String message) {
        super(message);
        this.serviceKey = serviceKey;
    }

}
