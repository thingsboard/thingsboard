// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class TbelScript {

    private final String scriptBody;
    private final String[] argNames;

    public Map createVars(Object[] args) {
        if (args == null || args.length != argNames.length) {
            throw new IllegalArgumentException("Invalid number of argument values");
        }
        var result = new HashMap<>();
        for (int i = 0; i < argNames.length; i++) {
            result.put(argNames[i], args[i]);
        }
        return result;
    }
}
