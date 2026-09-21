// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.math;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TbMathArgument {

    private String name;
    private TbMathArgumentType type;
    private String key;
    private String attributeScope;
    private Double defaultValue;

    public TbMathArgument(TbMathArgumentType type, String key) {
       this(key, type, key, null, null);
    }

    public TbMathArgument(String name, TbMathArgumentType type, String key) {
       this(name, type, key, null, null);
    }

}
