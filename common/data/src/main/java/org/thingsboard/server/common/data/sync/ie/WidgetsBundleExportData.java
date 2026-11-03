// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.server.common.data.widget.WidgetsBundle;

import java.util.ArrayList;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class WidgetsBundleExportData extends EntityExportData<WidgetsBundle> {

    @JsonProperty(index = 3)
    private List<ObjectNode> widgets;

    @JsonProperty(index = 4)
    private List<String> fqns;

    public void addFqn(String fqn) {
        if (fqns == null) {
            fqns = new ArrayList<>();
        }
        fqns.add(fqn);
    }

}
