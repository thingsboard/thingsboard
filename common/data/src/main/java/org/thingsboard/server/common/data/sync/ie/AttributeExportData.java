// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import lombok.Data;

@Data
public class AttributeExportData {
    private String key;
    private Long lastUpdateTs;

    private Boolean booleanValue;
    private String strValue;
    private Long longValue;
    private Double doubleValue;
    private String jsonValue;
}
