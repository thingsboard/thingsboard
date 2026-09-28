// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

/**
 * Immutable identity of a Citus worker node as discovered from {@code pg_dist_node}.
 *
 * <p>Worker identity is keyed on {@link #groupId()} — the canonical Citus worker-group id, which is
 * stable across host/NAT overrides and across rebalances (a worker keeps its group id even if the
 * coordinator advertises it under a different host/port to the app). {@code groupId == 0} is the
 * coordinator group and is never a routing target.
 *
 * <p>{@link #nodeName()} and {@link #nodePort()} are the raw catalog values exactly as the
 * coordinator reports them. Any host/NAT overrides (mapping the catalog nodename to an
 * app-reachable host) are applied later by the worker registry, not here.
 *
 * <p>{@code equals}/{@code hashCode} (provided by the record) intentionally include all components
 * so that a placement carrying stale host/port for the same group is not silently treated as equal.
 * Consumers that need worker-group identity should compare {@link #groupId()} directly.
 */
public record CitusWorkerNode(int groupId, String nodeName, int nodePort) {
}
