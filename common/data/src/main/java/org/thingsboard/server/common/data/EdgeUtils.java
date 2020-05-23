/**
 * Copyright © 2016-2020 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.data;

import org.thingsboard.server.common.data.id.EdgeId;

import java.util.Set;

public final class EdgeUtils {

    private EdgeUtils() {
    }

    public static boolean isAssignedToEdge(Set<ShortEdgeInfo> assignedEdges, EdgeId edgeId) {
        return assignedEdges != null && assignedEdges.contains(new ShortEdgeInfo(edgeId, null, null));
    }

    public static ShortEdgeInfo getAssignedEdgeInfo(Set<ShortEdgeInfo> assignedEdges, EdgeId edgeId) {
        if (assignedEdges != null) {
            for (ShortEdgeInfo edgeInfo : assignedEdges) {
                if (edgeInfo.getEdgeId().equals(edgeId)) {
                    return edgeInfo;
                }
            }
        }
        return null;
    }

    public static boolean addAssignedEdge(Set<ShortEdgeInfo> assignedEdges, ShortEdgeInfo edgeInfo) {
        if (assignedEdges != null && assignedEdges.contains(edgeInfo)) {
            return false;
        } else {
            if (assignedEdges != null) {
                assignedEdges.add(edgeInfo);
                return true;
            } else {
                return false;
            }
        }
    }

    public static boolean updateAssignedEdge(Set<ShortEdgeInfo> assignedEdges, ShortEdgeInfo edgeInfo) {
        if (assignedEdges != null && assignedEdges.contains(edgeInfo)) {
            assignedEdges.remove(edgeInfo);
            assignedEdges.add(edgeInfo);
            return true;
        } else {
            return false;
        }
    }

    public static boolean removeAssignedEdge(Set<ShortEdgeInfo> assignedEdges, ShortEdgeInfo edgeInfo) {
        if (assignedEdges != null && assignedEdges.contains(edgeInfo)) {
            assignedEdges.remove(edgeInfo);
            return true;
        } else {
            return false;
        }
    }
}
