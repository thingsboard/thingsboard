package org.thingsboard.rule.engine.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeofenceDurationConfig {

    private Map<UUID, GeofenceDuration> geofenceDurationMap;
    private TimeUnit timeUnit;

}
