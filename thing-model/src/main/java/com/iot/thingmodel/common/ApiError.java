package com.iot.thingmodel.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message, String code, List<Issue> issues, String retry) {
}
