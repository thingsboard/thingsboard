package org.thingsboard.rule.engine.geo;

import lombok.Data;

@Data
public class GeofenceDuration {

    private Long minInsideDuration;
    private Long minOutsideDuration;

}
