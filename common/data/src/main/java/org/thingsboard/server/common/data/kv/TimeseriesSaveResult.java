// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import lombok.Data;

import java.util.List;

@Data(staticConstructor = "of")
public class TimeseriesSaveResult {

    public static final TimeseriesSaveResult EMPTY = new TimeseriesSaveResult(0, null);

    private final Integer dataPoints;
    private final List<Long> versions;

}
