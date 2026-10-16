// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.geo;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.thingsboard.common.util.geo.PerimeterType;

import java.util.concurrent.TimeUnit;

/**
 * Created by ashvayka on 19.01.18.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TbGpsGeofencingActionNodeConfiguration extends TbGpsGeofencingFilterNodeConfiguration {

    private int minInsideDuration;
    private int minOutsideDuration;

    private String minInsideDurationTimeUnit;
    private String minOutsideDurationTimeUnit;

    private boolean reportPresenceStatusOnEachMessage;

    @Override
    public TbGpsGeofencingActionNodeConfiguration defaultConfiguration() {
        TbGpsGeofencingActionNodeConfiguration configuration = new TbGpsGeofencingActionNodeConfiguration();
        configuration.setLatitudeKeyName("latitude");
        configuration.setLongitudeKeyName("longitude");
        configuration.setPerimeterType(PerimeterType.POLYGON);
        configuration.setFetchPerimeterInfoFromMessageMetadata(true);
        configuration.setPerimeterKeyName("ss_perimeter");
        configuration.setMinInsideDurationTimeUnit(TimeUnit.MINUTES.name());
        configuration.setMinOutsideDurationTimeUnit(TimeUnit.MINUTES.name());
        configuration.setMinInsideDuration(1);
        configuration.setMinOutsideDuration(1);
        configuration.setReportPresenceStatusOnEachMessage(true);
        return configuration;
    }
}
