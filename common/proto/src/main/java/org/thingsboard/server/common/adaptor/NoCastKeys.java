// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.adaptor;

import com.fasterxml.jackson.core.type.TypeReference;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.server.common.data.StringUtils;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.msg.TbMsgMetaData;

import java.util.Collection;
import java.util.Collections;
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

    // A JSON array, since keys may contain commas.
    public static void put(TbMsgMetaData metaData, Collection<String> keys) {
        if (keys != null && !keys.isEmpty()) {
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
