// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.edge;

import org.junit.jupiter.api.Test;
import org.thingsboard.server.common.data.edge.Edge;
import org.thingsboard.server.common.data.id.EdgeProfileId;
import org.thingsboard.server.common.data.sync.ie.importing.csv.BulkImportColumnType;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EdgeBulkImportServiceTest {

    private final EdgeBulkImportService service = new EdgeBulkImportService(null, null);

    @Test
    void typeColumnDecidesTheProfileOfAnEdgeThatAlreadyHasOne() {
        Edge edge = new Edge();
        edge.setEdgeProfileId(new EdgeProfileId(UUID.randomUUID()));

        service.setEntityFields(edge, Map.of(BulkImportColumnType.TYPE, "gateway"));

        assertThat(edge.getType()).isEqualTo("gateway");
        // the stored profile id would win over the column when the edge is saved
        assertThat(edge.getEdgeProfileId()).isNull();
    }

    @Test
    void anImportWithoutATypeColumnLeavesTheProfileAlone() {
        EdgeProfileId profileId = new EdgeProfileId(UUID.randomUUID());
        Edge edge = new Edge();
        edge.setEdgeProfileId(profileId);

        service.setEntityFields(edge, Map.of(BulkImportColumnType.LABEL, "shop floor"));

        assertThat(edge.getEdgeProfileId()).isEqualTo(profileId);
    }
}
