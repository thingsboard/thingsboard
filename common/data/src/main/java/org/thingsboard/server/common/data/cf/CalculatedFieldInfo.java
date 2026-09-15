// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.cf;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CalculatedFieldInfo extends CalculatedField {

    private String entityName;

    public CalculatedFieldInfo(CalculatedField calculatedField, String entityName) {
        super(calculatedField);
        this.entityName = entityName;
    }

}
