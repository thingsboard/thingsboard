// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.monitoring.data;

import lombok.Data;

@Data(staticConstructor = "of")
public class Latency {
    private final String key;
    private final double value;

    public String getFormattedValue() {
        return String.format("%,.2f ms", value);
    }

}
