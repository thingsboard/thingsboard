// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.install.migrate;

import lombok.Data;

@Data
public class CassandraToSqlColumnData {

    private String value;
    private String originalValue;
    private int constraintCounter = 0;

    public CassandraToSqlColumnData(String value) {
        this.value = value;
        this.originalValue = value;
    }

    public int nextContraintCounter() {
        return ++constraintCounter;
    }

    public String getNextConstraintStringValue(CassandraToSqlColumn column) {
        int counter = this.nextContraintCounter();
        String newValue = this.originalValue + counter;
        int overflow = newValue.length() - column.getSize();
        if (overflow > 0) {
            newValue = this.originalValue.substring(0, this.originalValue.length()-overflow) + counter;
        }
        return newValue;
    }

    public String getNextConstraintEmailValue(CassandraToSqlColumn column) {
        int counter = this.nextContraintCounter();
        String[] emailValues = this.originalValue.split("@");
        String newValue = emailValues[0] + "+" + counter + "@" + emailValues[1];
        int overflow = newValue.length() - column.getSize();
        if (overflow > 0) {
            newValue = emailValues[0].substring(0, emailValues[0].length()-overflow) + "+" + counter + "@" + emailValues[1];
        }
        return newValue;
    }

    public String getLogValue() {
        if (this.value != null && this.value.length() > 255) {
            return this.value.substring(0, 255) + "...[truncated " + (this.value.length() - 255) + " symbols]";
        }
        return this.value;
    }

}
