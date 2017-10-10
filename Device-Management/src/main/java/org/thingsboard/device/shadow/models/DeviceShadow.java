package org.thingsboard.device.shadow.models;

import javax.persistence.*;

/**
 * Created by himanshu on 29/9/17.
 */

public class DeviceShadow {

    /*@Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;*/
    private String deviceToken;
    private String tagName;
    private Boolean desired;
    private Boolean reported;

    public DeviceShadow(String deviceToken, String tagName, Boolean desired, Boolean reported){
        this.deviceToken = deviceToken;
        this.tagName = tagName;
        this.desired = desired;
        this.reported = reported;
    }


    public String getDeviceToken() {
        return deviceToken;
    }

    public String getTagName() {
        return tagName;
    }

    public Boolean getDesired() {
        return desired;
    }

    public Boolean getReported() {
        return reported;
    }
}
