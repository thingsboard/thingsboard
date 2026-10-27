// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.limit;

public record RateLimitEntry(long capacity, long durationSeconds) {

    public static RateLimitEntry parse(String s) {
        String[] parts = s.split(":");
        return new RateLimitEntry(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
    }

    @Override
    public String toString() {
        return capacity + ":" + durationSeconds;
    }

}
