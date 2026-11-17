// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.adaptor;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.common.data.kv.KvEntry;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Carries the keys whose numeric-looking string values must stay strings, from an integration uplink
 * or an attributes updated notification, to the rule nodes that parse the message data again with {@link JsonConverter}.
 */
public final class TypeCastDisabledKeys {

    public static final String METADATA_KEY = "typeCastDisabledKeys";

    private static final TypeReference<Set<String>> SET_TYPE = new TypeReference<>() {};

    private TypeCastDisabledKeys() {
    }

    // A JSON array, since keys may contain commas.
    public static void put(TbMsgMetaData metaData, Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
            metaData.putValue(METADATA_KEY, JacksonUtil.toString(keys));
        }
    }

    /**
     * Marks the string entries that {@link JsonConverter} would cast to a number, so that a message
     * built from typed entries, such as an attributes updated notification, reads back the same types.
     */
    public static void putNumericStrings(TbMsgMetaData metaData, Collection<? extends KvEntry> entries) {
        if (entries == null) {
            return;
        }
        put(metaData, entries.stream()
                .filter(entry -> entry.getDataType() == DataType.STRING && NumberUtils.isParsable(entry.getValueAsString()))
                .map(KvEntry::getKey)
                .collect(Collectors.toList()));
    }

    static Set<String> get(TbMsgMetaData metaData) {
        String value = metaData.getValue(METADATA_KEY);
        if (StringUtils.isEmpty(value)) {
            return Collections.emptySet();
        }
        try {
            Set<String> keys = JacksonUtil.fromString(value, SET_TYPE);
            return keys != null ? keys : Collections.emptySet();
        } catch (IllegalArgumentException e) {
            return Collections.emptySet();
        }
    }

}
