/**
 * Copyright © 2016-2018 The Thingsboard Authors
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
@NoArgsConstructor
public final class TbMsgMetaData implements Serializable {

    private final Map<String, String> data = new ConcurrentHashMap<>();

    public TbMsgMetaData(Map<String, String> data) {
        this.data.putAll(data);
    }

    public String getValue(String key) {
        return data.get(key);
    }

    public void putValue(String key, String value) {
        data.put(key, value);
    }

    public Map<String, String> values() {
        return new HashMap<>(data);
    }

    public TbMsgMetaData copy() {
        return new TbMsgMetaData(new ConcurrentHashMap<>(data));
    }
}
