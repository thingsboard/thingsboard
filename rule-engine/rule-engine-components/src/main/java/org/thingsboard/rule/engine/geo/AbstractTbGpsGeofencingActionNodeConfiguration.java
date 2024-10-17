package org.thingsboard.rule.engine.geo;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class AbstractTbGpsGeofencingActionNodeConfiguration<T extends AbstractTbGpsGeofencingActionNodeConfiguration<T>> extends AbstractTbGpsGeofencingNodeConfiguration<T> {

    private int minInsideDuration;
    private int minOutsideDuration;
    private String minInsideDurationTimeUnit;
    private String minOutsideDurationTimeUnit;

}
