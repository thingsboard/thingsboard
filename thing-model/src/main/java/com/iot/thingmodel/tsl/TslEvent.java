package com.iot.thingmodel.tsl;

import java.util.List;

public record TslEvent(String id, String name, String desc, String type, List<TslParam> params) {
}
