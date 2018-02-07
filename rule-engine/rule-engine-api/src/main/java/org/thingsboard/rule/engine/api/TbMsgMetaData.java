package org.thingsboard.rule.engine.api;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * Created by ashvayka on 13.01.18.
 */
@Data
public final class TbMsgMetaData implements Serializable {

    private Map<String, String> data;

    public String getValue(String key) {
        return data.get(key);
    }

    public void putValue(String key, String value) {
        data.put(key, value);
    }

}
