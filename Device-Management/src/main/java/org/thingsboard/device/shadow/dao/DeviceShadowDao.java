package org.thingsboard.device.shadow.dao;

import org.thingsboard.device.shadow.models.DeviceShadow;
import org.thingsboard.device.shadow.models.TagList;

import java.sql.SQLException;

/**
 * Created by himanshu on 29/9/17.
 */
public interface DeviceShadowDao {
    public String insertAvailableTags(DeviceShadow deviceShadow) throws SQLException;
    public String updateAvailableTags(DeviceShadow deviceShadow) throws SQLException;
    //public boolean deleteByToken(String deviceToken) throws SQLException;
    public void updateDeviceState(TagList tagList, String tagType) throws  SQLException;
    public TagList getAvailableTagsForDevice(String deviceName) throws SQLException;
    public boolean ifDeviceExists(String deviceName) throws SQLException;
    public DeviceShadow getDeviceShadow(String deviceName) throws SQLException;
}
