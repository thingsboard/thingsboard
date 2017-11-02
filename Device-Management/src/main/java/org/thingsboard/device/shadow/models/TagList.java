package org.thingsboard.device.shadow.models;

import java.util.List;

public class TagList {
    private String deviceName;
    private List<String> tags;

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTags() {
        return tags;
    }
}
