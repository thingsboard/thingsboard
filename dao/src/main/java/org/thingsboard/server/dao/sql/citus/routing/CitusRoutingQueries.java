// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.sql.citus.routing;

/**
 * Shared SQL fragments for Citus smart-routing catalog queries, so the same semantic predicate is not
 * duplicated (and at risk of drifting) across {@link CitusShardPlacement} and {@link CitusWorkerRegistry}.
 */
final class CitusRoutingQueries {

    private CitusRoutingQueries() {
    }

    /**
     * Predicate selecting an active primary worker node in {@code pg_dist_node}, excluding the
     * coordinator group ({@code groupid = 0}). The columns are referenced through the supplied table
     * alias prefix so the same fragment serves both an aliased join (e.g. {@code n.}) and an unaliased
     * single-table scan.
     *
     * @param aliasPrefix the column prefix including the trailing dot (e.g. {@code "n."}), or an empty
     *                    string for an unaliased query
     */
    static String activePrimaryWorkerPredicate(String aliasPrefix) {
        return aliasPrefix + "noderole = 'primary' and " + aliasPrefix + "isactive and " + aliasPrefix + "groupid <> 0";
    }
}
