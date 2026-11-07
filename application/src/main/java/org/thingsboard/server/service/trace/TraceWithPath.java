// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.trace;

/**
 * A drained trace with its path, built once per aggregation run and shared by the group aggregator and the summary
 * builder.
 */
public record TraceWithPath(BufferedTrace trace, TracePath path) {
}
