package org.thingsboard.device.shadow.models;

import java.util.List;

/**
 * Created by himanshu on 30/10/17.
 */
public class DeviceShadow {
    private String deviceName;
    private String availableTags;
    private String desiredTags;
    private String reportedTags;

    public DeviceShadow(String deviceName, String availableTags, String desiredTags,
                        String reportedTags){
        this.deviceName = deviceName;
        this.availableTags = availableTags;
        this.desiredTags = desiredTags;
        this.reportedTags = reportedTags;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public void setAvailableTags(String availableTags) {
        this.availableTags = availableTags;
    }

    public void setDesiredTags(String desiredTags) {
        this.desiredTags = desiredTags;
    }

    public void setReportedTags(String reportedTags) {
        this.reportedTags = reportedTags;
    }

    public String getAvailableTags() {
        return availableTags;
    }

    public String getDesiredTags() {
        return desiredTags;
    }

    public String getReportedTags() {
        return reportedTags;
    }

}
