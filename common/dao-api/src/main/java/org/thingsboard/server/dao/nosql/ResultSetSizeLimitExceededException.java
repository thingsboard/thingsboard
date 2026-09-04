// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.nosql;

import lombok.Getter;

@Getter
public class ResultSetSizeLimitExceededException extends IllegalArgumentException {

    private final long limitBytes;
    private final long actualBytes;

    public ResultSetSizeLimitExceededException(long limitBytes, long actualBytes) {
        super("Result set size exceeds the maximum allowed limit. Please narrow your query");
        this.limitBytes = limitBytes;
        this.actualBytes = actualBytes;
    }

}
