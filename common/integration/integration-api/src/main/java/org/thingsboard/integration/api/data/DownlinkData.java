// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.data;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class DownlinkData {

    private final String contentType;
    private final byte[] data;
    private final Map<String, String> metadata;

    public boolean isEmpty() {
        return data == null || data.length == 0;
    }

}
