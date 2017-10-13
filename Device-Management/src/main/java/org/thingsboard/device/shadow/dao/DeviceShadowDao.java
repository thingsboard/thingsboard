package org.thingsboard.device.shadow.dao;

import org.thingsboard.device.shadow.models.DeviceShadow;
import org.thingsboard.device.shadow.models.TagList;

import java.sql.SQLException;

/**
 * Created by himanshu on 29/9/17.
 */
public interface DeviceShadowDao {
    //@Query("select s from Student s where s.age <= ?")
    public void updateByDeviceToken(String token);
    public TagList getReportedTagsForDeviceToken(String token) throws SQLException;
    public void updateAvailableTags(DeviceShadow deviceShadow) throws SQLException;
    //public boolean deleteByToken(String deviceToken) throws SQLException;
    public void updateDeviceState(TagList tagList) throws  SQLException;
    public boolean checkIfTagExists(String token, String tag) throws SQLException;
    public void updateReportedTags(String token, String tag) throws SQLException;
    public TagList getAllTagsForDeviceToken(String token) throws SQLException;
}
