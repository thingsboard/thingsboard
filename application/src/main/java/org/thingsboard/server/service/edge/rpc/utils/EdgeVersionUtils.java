// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.edge.rpc.utils;

import lombok.extern.slf4j.Slf4j;
import org.thingsboard.edge.rpc.EdgeVersionComparator;
import org.thingsboard.server.gen.edge.v1.EdgeVersion;

@Slf4j
public final class EdgeVersionUtils {

    public static boolean isEdgeVersionOlderThan(EdgeVersion currentVersion, EdgeVersion requiredVersion) {
        return EdgeVersionComparator.INSTANCE.compare(currentVersion, requiredVersion) < 0;
    }

}
