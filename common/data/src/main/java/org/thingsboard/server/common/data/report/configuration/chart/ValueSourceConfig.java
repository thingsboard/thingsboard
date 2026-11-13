// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.chart;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.report.configuration.DataKey;

@Data
public abstract class ValueSourceConfig {

    private ValueSourceType type;
    private Double value;
    private String latestKeyType;
    private String latestKey;
    private String entityKeyType;
    private String entityAlias;
    private String entityKey;

    @JsonIgnore
    public boolean isValidSource() {
        if (type == null) {
            return false;
        }
        switch (type) {
            case constant -> {
                return value != null;
            }
            case latestKey -> {
                return ("attribute".equals(latestKeyType) || "timeseries".equals(latestKeyType)) && StringUtils.isNotBlank(latestKey);
            }
            case entity -> {
                return ("attribute".equals(entityKeyType) || "timeseries".equals(entityKeyType)) && StringUtils.isNotBlank(entityAlias) && StringUtils.isNotBlank(entityKey);
            }
        }
        return false;
    }

    @JsonIgnore
    public DataKey toEntityDataKey() {
        DataKey key = new DataKey();
        key.setName(entityKey);
        key.setType(entityKeyType);
        return key;
    }

}
