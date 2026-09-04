// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.thingsboard.server.common.data.HasVersion;
import org.thingsboard.server.common.data.query.TsValue;

/**
 * Represents time series KV data entry
 *
 * @author ashvayka
 *
 */
public interface TsKvEntry extends KvEntry, HasVersion {

    long getTs();

    @JsonIgnore
    int getDataPoints();

    @JsonIgnore
    default TsValue toTsValue() {
        return new TsValue(getTs(), getValueAsString());
    }

    @JsonIgnore
    default boolean isDeletedEntry() {
        return getTs() == 0 && (getValue() == null || getValueAsString().isEmpty());
    }

}
