// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.downlink;

import org.eclipse.leshan.core.request.ContentFormat;

import java.util.Optional;

public interface HasContentFormat {

    Optional<ContentFormat> getRequestContentFormat();

    default ContentFormat getResponseContentFormat() {
        return null;
    }
}
