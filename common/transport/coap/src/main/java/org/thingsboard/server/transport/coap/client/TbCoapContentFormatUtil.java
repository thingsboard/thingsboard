// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.coap.client;

import org.eclipse.californium.core.coap.MediaTypeRegistry;

public class TbCoapContentFormatUtil {

    public static int getContentFormat(int requestFormat, int adaptorFormat) {
        if (isStrict(adaptorFormat)) {
            return adaptorFormat;
        } else {
            return requestFormat != MediaTypeRegistry.UNDEFINED ? requestFormat : adaptorFormat;
        }
    }

    public static boolean isStrict(int contentFormat) {
        return contentFormat == MediaTypeRegistry.APPLICATION_OCTET_STREAM;
    }
}
