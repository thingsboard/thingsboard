package org.thingsboard.server.dao.model.sql.ruleenginemonitoring;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BufferedSpan {
    @JsonIgnore
    private String tenantId;
    @JsonIgnore
    private String traceId;

    private String spanId;
    private String parentSpanId;
    private String name;
    private String serviceName;
    private JsonNode attributesJsonb;
    private JsonNode eventsJsonb;
    private Long startTime;
    private Long endTime;
    private String statusCode;
    private String statusMessage;
    private long createdTime;
}
