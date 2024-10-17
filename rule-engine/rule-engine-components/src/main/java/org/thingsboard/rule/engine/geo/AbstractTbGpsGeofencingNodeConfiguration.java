package org.thingsboard.rule.engine.geo;

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public abstract class AbstractTbGpsGeofencingNodeConfiguration<T extends AbstractTbGpsGeofencingNodeConfiguration<T>> implements NodeConfiguration<T> {

    private String latitudeKeyName;
    private String longitudeKeyName;
    private PerimeterType perimeterType;
    private boolean fetchPerimeterInfoFromMessageMetadata;
    private String perimeterKeyName;

    // For Polygons
    private String polygonsDefinition;

    // For Circles
    private Double centerLatitude;
    private Double centerLongitude;
    private Double range;
    private RangeUnit rangeUnit;

}
