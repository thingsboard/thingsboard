// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TbMathResult {

    private TbMathArgumentType type;
    private String key;
    // 0 means integer, x > 0 means x decimal points after ".";
    private int resultValuePrecision;
    private boolean addToBody;
    private boolean addToMetadata;
    private String attributeScope;

}
