// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.script.api.tbel;

import lombok.Getter;

import java.util.Collections;
import java.util.Map;

public class TbelCfCtx implements TbelCfObject {

    @Getter
    private final Map<String, TbelCfArg> args;
    @Getter
    private final long latestTs;

    public TbelCfCtx(Map<String, TbelCfArg> args, long latestTs) {
        this.args = Collections.unmodifiableMap(args);
        this.latestTs = latestTs != -1 ? latestTs : System.currentTimeMillis();
    }

    @Override
    public long memorySize() {
        return OBJ_SIZE;
    }
}
