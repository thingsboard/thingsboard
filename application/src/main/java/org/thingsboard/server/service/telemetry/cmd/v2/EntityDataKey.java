package org.thingsboard.server.service.telemetry.cmd.v2;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = EntityDataKeyDeserializer.class)
public class EntityDataKey {

    private final String key;
    private final boolean dataConversion;

    public String getKey() {
        return key;
    }

    public boolean isDataConversion() {
        return dataConversion;
    }

    public EntityDataKey(String key) {
        this.key = key;
        this.dataConversion = false;
    }

    public EntityDataKey(String key, boolean dataConversion) {
        this.key = key;
        this.dataConversion = dataConversion;
    }

}
