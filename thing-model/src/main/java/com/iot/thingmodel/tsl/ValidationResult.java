package com.iot.thingmodel.tsl;

import com.iot.thingmodel.common.Issue;
import java.util.List;

public record ValidationResult(List<Issue> errors, List<Issue> warnings) {

    public ValidationResult {
        errors = errors == null ? List.of() : List.copyOf(errors);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public boolean valid() {
        return errors.isEmpty();
    }
}
