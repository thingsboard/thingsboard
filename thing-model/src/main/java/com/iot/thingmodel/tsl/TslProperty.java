package com.iot.thingmodel.tsl;

import com.fasterxml.jackson.databind.JsonNode;

public record TslProperty(
        String id,
        String name,
        String desc,
        String mode,
        boolean required,
        TslDefine define,
        JsonNode mapping) {
}
