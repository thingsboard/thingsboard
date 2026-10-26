package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;

public record TslDefine(
        String type,
        Double min,
        Double max,
        Double start,
        Double step,
        String unit,
        JsonNode specs,
        JsonNode mapping) {
}
