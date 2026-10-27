// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edge;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.id.EdgeId;

@Data
@EqualsAndHashCode(callSuper = true)
public class EdgeInfo extends Edge {

    private String customerTitle;
    private boolean customerIsPublic;

    public EdgeInfo() {
        super();
    }

    public EdgeInfo(EdgeId edgeId) {
        super(edgeId);
    }

    public EdgeInfo(Edge edge, String customerTitle, boolean customerIsPublic) {
        super(edge);
        this.customerTitle = customerTitle;
        this.customerIsPublic = customerIsPublic;
    }
}