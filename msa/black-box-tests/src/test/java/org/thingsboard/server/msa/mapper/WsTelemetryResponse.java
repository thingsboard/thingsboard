// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.msa.mapper;

import lombok.Data;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
public class WsTelemetryResponse implements Serializable {
    private int subscriptionId;
    private int errorCode;
    private String errorMsg;
    private Map<String, List<List<Object>>> data;
    private Map<String, Object> latestValues;

    public List<Object> getDataValuesByKey(String key) {
        return data.entrySet().stream()
                .filter(e -> e.getKey().equals(key))
                .flatMap(e -> e.getValue().stream().flatMap(Collection::stream))
                .collect(Collectors.toList());
    }
}
