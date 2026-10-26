package com.iot.thingmodel.product;

import com.fasterxml.jackson.databind.JsonNode;
import com.iot.thingmodel.common.Constants;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateThingModelRequest(
        @Size(max = Constants.Validation.TSL_VERSION_MAX_LENGTH) String tslVersion,
        @NotNull JsonNode tsl,
        Boolean strictUnknown) {
}
