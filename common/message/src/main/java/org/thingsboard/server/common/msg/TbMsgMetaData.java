/**
 * Copyright © 2016-2025 The Thingsboard Authors
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

import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
public final class TbMsgMetaData implements Serializable {

    public static final TbMsgMetaData EMPTY = new TbMsgMetaData(0);

    private final Map<String, String> data;

    public TbMsgMetaData() {
        this.data = new ConcurrentHashMap<>();
    }

    public TbMsgMetaData(Map<String, String> data) {
        this.data = new ConcurrentHashMap<>();
        data.forEach(this::putValue);
    }

    /**
     * Internal constructor to create immutable TbMsgMetaData.EMPTY
     * */
    private TbMsgMetaData(int ignored) {
        this.data = Collections.emptyMap();
    }

    public String getValue(String key) {
        return this.data.get(key);
    }

    public void putValue(String key, String value) {
        if (key != null && value != null) {
            this.data.put(key, value);
        }
    }

    public Map<String, String> values() {
        return new HashMap<>(this.data);
    }

    public TbMsgMetaData copy() {
        return new TbMsgMetaData(this.data);
    }
}
