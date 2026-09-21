// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.dao.model.sql;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.edge.Edge;

import static org.thingsboard.server.dao.model.ModelConstants.EDGE_TABLE_NAME;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = EDGE_TABLE_NAME)
public class EdgeEntity extends AbstractEdgeEntity<Edge> {

    public EdgeEntity() {
        super();
    }

    public EdgeEntity(Edge edge) {
        super(edge);
    }

    @Override
    public Edge toData() {
        return super.toEdge();
    }
}
