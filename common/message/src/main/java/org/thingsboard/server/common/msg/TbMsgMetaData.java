// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.thingsboard.server.common.data.StringUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public final class TbMsgMetaData implements Serializable {

    public static final TbMsgMetaData EMPTY = new TbMsgMetaData(0);

    /**
     * Carries the keys whose numeric-looking string values must stay strings, from an integration uplink or an attributes
     * updated notification, to the rule nodes that parse the message data again with {@code JsonConverter}.
     */
    public static final String TYPE_CAST_DISABLED_KEYS = "typeCastDisabledKeys";

    private static final ObjectMapper TYPE_CAST_DISABLED_KEYS_MAPPER = new ObjectMapper();
    private static final TypeReference<Set<String>> TYPE_CAST_DISABLED_KEYS_TYPE = new TypeReference<>() {};

    private final Map<String, String> data;

    public TbMsgMetaData() {
        data = new ConcurrentHashMap<>();
    }

    public TbMsgMetaData(Map<String, String> data) {
        this.data = new ConcurrentHashMap<>();
        data.forEach(this::putValue);
    }

    /**
     * Internal constructor to create immutable TbMsgMetaData.EMPTY
     * */
    private TbMsgMetaData(int ignored) {
        data = Collections.emptyMap();
    }

    public String getValue(String key) {
        return data.get(key);
    }

    public void putValue(String key, String value) {
        if (key != null && value != null) {
            data.put(key, value);
        }
    }

    // A JSON array, since keys may contain commas.
    public void putTypeCastDisabledKeys(Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            try {
                putValue(TYPE_CAST_DISABLED_KEYS, TYPE_CAST_DISABLED_KEYS_MAPPER.writeValueAsString(keys));
            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("The given keys cannot be written as a JSON array: " + keys, e);
            }
        }
    }

    /**
     * Returns the keys written by {@link #putTypeCastDisabledKeys(Collection)}; empty when there are none or the value is not a JSON array.
     */
    @JsonIgnore
    public Set<String> getTypeCastDisabledKeys() {
        String value = getValue(TYPE_CAST_DISABLED_KEYS);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptySet();
        }
        try {
            Set<String> keys = TYPE_CAST_DISABLED_KEYS_MAPPER.readValue(value, TYPE_CAST_DISABLED_KEYS_TYPE);
            return keys != null ? keys : Collections.emptySet();
        } catch (JsonProcessingException e) {
            return Collections.emptySet();
        }
    }

    public Map<String, String> values() {
        return new HashMap<>(data);
    }

    public TbMsgMetaData copy() {
        return new TbMsgMetaData(data);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return data == null || data.isEmpty();
    }

}
