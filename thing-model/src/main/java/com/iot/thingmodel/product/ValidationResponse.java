package com.iot.thingmodel.product;

import com.iot.thingmodel.common.Issue;
import java.util.List;

public record ValidationResponse(boolean valid, List<Issue> errors, List<Issue> warnings) {

    public ValidationResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
