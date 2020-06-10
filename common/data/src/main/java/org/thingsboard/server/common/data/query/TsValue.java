package org.thingsboard.server.common.data.query;

import lombok.Data;

@Data
public class TsValue {

    private final long ts;
    private final String value;
}
