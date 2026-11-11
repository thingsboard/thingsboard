// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.logexternal;

import org.thingsboard.server.cache.logexternal.LogChunkBuffer;
import org.thingsboard.server.cache.logexternal.LogChunkCaffeineBuffer;

public class LogChunkCaffeineBufferContractTest extends AbstractLogChunkBufferContractTest {

    private final LogChunkBuffer buffer = new LogChunkCaffeineBuffer(900, MAX_CHUNKS_PER_UNIT, 64 * 1024 * 1024);

    @Override
    protected LogChunkBuffer buffer() {
        return buffer;
    }
}
