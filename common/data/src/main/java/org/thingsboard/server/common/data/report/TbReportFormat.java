// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report;

public enum TbReportFormat {

    // WARNING: the ordinal is persisted - Report.format has no @Enumerated, so JPA stores it ORDINAL,
    // and JpaReportDao#getReportDataByPublicKey reads the raw ordinal via fromOrdinal(). Do NOT reorder
    // these constants or insert new ones in the middle; only append, otherwise existing rows change meaning.
    PDF("application/pdf", ".pdf"), CSV("text/csv", ".csv");

    private final String contentType;
    private final String extension;

    TbReportFormat(String contentType, String extension) {
        this.contentType = contentType;
        this.extension = extension;
    }

    public String getContentType() {
        return contentType;
    }

    public String getExtension() {
        return extension;
    }

    public static TbReportFormat fromOrdinal(int ordinal) {
        TbReportFormat[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            throw new IllegalArgumentException("Unknown report format ordinal: " + ordinal);
        }
        return values[ordinal];
    }

}
