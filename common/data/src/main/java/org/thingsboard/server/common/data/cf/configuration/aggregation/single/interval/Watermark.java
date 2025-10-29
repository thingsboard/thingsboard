package org.thingsboard.server.common.data.cf.configuration.aggregation.single.interval;

import lombok.Data;

@Data
public class Watermark {

    private long duration;
    private long checkInterval;

}
