// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.sync.ie.importing.csv;

import lombok.Data;

import java.util.List;

@Data
public class BulkImportRequest {
    private String file;
    private Mapping mapping;

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
