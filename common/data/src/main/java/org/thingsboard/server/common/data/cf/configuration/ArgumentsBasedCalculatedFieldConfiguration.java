package org.thingsboard.server.common.data.cf.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

public interface ArgumentsBasedCalculatedFieldConfiguration extends CalculatedFieldConfiguration {

    Map<String, Argument> getArguments();

    String getExpression();

    void setExpression(String expression);

    Output getOutput();

    @JsonIgnore
    default boolean isScheduledUpdateEnabled() {
        return false;
    }

    default void setScheduledUpdateIntervalSec(int scheduledUpdateIntervalSec) {
    }

    default int getScheduledUpdateIntervalSec() {
        return 0;
    }

}
