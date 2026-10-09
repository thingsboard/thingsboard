// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.data.sync.ie.importing.csv;

import lombok.Data;
import org.thingsboard.server.common.data.id.CustomerId;

import java.util.List;

@Data
public class BulkImportRequest {
    private String file;
    private Mapping mapping;
    private CustomerId customerId;
    private String entityGroupId;

    @Data
    public static class Mapping {
        private List<ColumnMapping> columns;
        private Character delimiter;
        private Boolean update;
        private Boolean header;
    }

    @Data
    public static class ColumnMapping {
        private BulkImportColumnType type;
        private String key;
    }

}
