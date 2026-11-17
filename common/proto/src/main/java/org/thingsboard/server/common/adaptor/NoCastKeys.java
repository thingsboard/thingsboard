// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.adaptor;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.lang3.math.NumberUtils;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueProto;
import org.thingsboard.server.gen.transport.TransportProtos.KeyValueType;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Carries the keys whose numeric-looking string values must stay strings from an integration uplink
 * to the rule nodes that parse the message data again with {@link JsonConverter}.
 */
public final class NoCastKeys {

    public static final String METADATA_KEY = "noCastKeys";

    private static final TypeReference<Set<String>> SET_TYPE = new TypeReference<>() {};

    private NoCastKeys() {
    }

    // The converter already cast every other numeric-looking string, so a STRING_V that still parses was kept on purpose.
    public static Set<String> fromKvList(List<KeyValueProto> kvList) {
        Set<String> keys = new LinkedHashSet<>();
        for (KeyValueProto kv : kvList) {
            if (kv.getType() == KeyValueType.STRING_V && NumberUtils.isParsable(kv.getStringV())) {
                keys.add(kv.getKey());
            }
        }
        return keys;
    }

    public static void put(TbMsgMetaData metaData, List<KeyValueProto> kvList) {
        put(metaData, fromKvList(kvList));
    }

    // A JSON array, since keys may contain commas.
    public static void put(TbMsgMetaData metaData, Set<String> keys) {
        if (!keys.isEmpty()) {
            metaData.putValue(METADATA_KEY, JacksonUtil.toString(keys));
        }
    }

    public static Set<String> get(TbMsg msg) {
        return get(msg.getMetaData());
    }

    public static Set<String> get(TbMsgMetaData metaData) {
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
