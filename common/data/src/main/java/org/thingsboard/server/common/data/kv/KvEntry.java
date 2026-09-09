// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.kv;

import java.io.Serializable;
import java.util.Optional;

/**
 * Represents attribute or any other KV data entry
 *
 * @author ashvayka
 */
public interface KvEntry extends Serializable {

    String getKey();

    DataType getDataType();

    Optional<String> getStrValue();

    Optional<Long> getLongValue();

    Optional<Boolean> getBooleanValue();

    Optional<Double> getDoubleValue();

    Optional<String> getJsonValue();

    String getValueAsString();

    Object getValue();
}
