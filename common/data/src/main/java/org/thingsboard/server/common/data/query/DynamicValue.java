// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.query;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.thingsboard.server.common.data.validation.NoXss;

import java.io.Serializable;

@Data
@RequiredArgsConstructor
public class DynamicValue<T> implements Serializable {

    private T resolvedValue;

    private final DynamicValueSourceType sourceType;
    @NoXss
    private final String sourceAttribute;
    private final boolean inherit;

    public DynamicValue(DynamicValueSourceType sourceType, String sourceAttribute) {
        this.sourceAttribute = sourceAttribute;
        this.sourceType = sourceType;
        this.inherit = false;
    }

}
