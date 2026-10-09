// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.common.util;

/**
 * Maps a hash code to a non-negative partition index in {@code [0, partitions)}.
 */
public final class HashPartitioner {

    private HashPartitioner() {
    }

    public static int resolvePartition(int hashCode, int partitions) {
        if (partitions <= 0) {
            throw new IllegalArgumentException("partitions must be > 0, but was " + partitions);
        }
        return (hashCode & 0x7FFFFFFF) % partitions;
    }
}
