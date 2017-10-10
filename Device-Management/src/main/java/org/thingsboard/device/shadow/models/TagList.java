package org.thingsboard.device.shadow.models;

import java.util.List;

/**
 * Created by himanshu on 4/10/17.
 */
public class TagList {
    private String token;
    private List<String> tags;

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getTags() {
        return tags;
    }
}
