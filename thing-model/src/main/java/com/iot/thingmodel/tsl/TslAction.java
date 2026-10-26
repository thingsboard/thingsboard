package com.iot.thingmodel.tsl;

import java.util.List;

public record TslAction(String id, String name, String desc, List<TslParam> input, List<TslParam> output) {
}
