/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.server.common.msg;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public final class TbMsgMetaData implements Serializable {

    public static final TbMsgMetaData EMPTY = new TbMsgMetaData(0);

    /**
     * Reserved metadata key. Populated by the platform (e.g. the Rule Engine REST API
     * v2 enrichment) with a JSON array of {@code EntityAclEntry}. Rule chains MUST
     * treat this value as authoritative; the platform overwrites any caller-supplied
     * value of this key before the message is forwarded to the Rule Engine.
     */
    public static final String TB_ACL_SNAPSHOT_KEY = "tb_aclSnapshot";

    /**
     * Reserved metadata key. Populated by the platform with the UUID (as string) of
     * the user that initiated the request — intended for audit logging inside rule
     * chains. The platform overwrites any caller-supplied value of this key.
     */
    public static final String TB_USER_ID_KEY = "tb_userId";

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
