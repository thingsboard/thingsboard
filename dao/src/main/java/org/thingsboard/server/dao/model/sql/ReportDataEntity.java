// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.dao.model.sql;

public interface ReportDataEntity {
    byte[] getData();
    String getName();
    int getFormat();
}
