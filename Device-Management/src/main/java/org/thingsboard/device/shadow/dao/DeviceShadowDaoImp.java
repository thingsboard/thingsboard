package org.thingsboard.device.shadow.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.thingsboard.device.shadow.components.ConfigProperties;
import org.thingsboard.device.shadow.models.DeviceShadow;
import org.thingsboard.device.shadow.models.TagList;

import javax.annotation.PostConstruct;
import java.sql.*;
import java.util.Arrays;
import java.util.List;

@Repository("deviceShadowDao")
public class DeviceShadowDaoImp implements DeviceShadowDao {

    private Logger logger = LoggerFactory.getLogger(DeviceShadowDao.class);
    private String shadowTable = "DEVICE_SHADOW";

    @Autowired
    private ConfigProperties configProperties;

    Connection connection = null;

    @PostConstruct
    public void createDbConnection(){
        try {
            String DB_URL = configProperties.getDB_URL();
            String USER = configProperties.getUSER();
            String PASS = configProperties.getPASS();
            connection = DriverManager.getConnection(DB_URL,USER,PASS);
        }catch (Exception e){
            e.printStackTrace();
            logger.error("Error in creating database connection " + e);
        }
    }

    final String insertAvailableTags = "INSERT INTO " + shadowTable +
            " (deviceName,availableTags,desiredTags,reportedTags) VALUES (?, ?, ?, ?)";

    final String updateAvailableTags = "UPDATE " + shadowTable + " set availableTags=? where " +
            "DEVICENAME=?";

    final String fetchAvailableTags = "SELECT AVAILABLETAGS from "+ shadowTable +" where " +
            "DEVICENAME = ?";

    final String updateDesiredTags = "UPDATE " + shadowTable + " set DESIREDTAGS=? where " +
            "DEVICENAME=?";

    final String updateReportedTags = "UPDATE " + shadowTable + " set REPORTEDTAGS=? where " +
            "DEVICENAME=?";

    final String checkDeviceExistence = "SELECT count(*) FROM " + shadowTable + " WHERE "
            + "DEVICENAME=?";

    final String getDeviceShadow = "SELECT * from " + shadowTable + " WHERE DEVICENAME=?";


    public String insertAvailableTags(DeviceShadow deviceShadow) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(insertAvailableTags);
        ps.setString(1, deviceShadow.getDeviceName());
        ps.setString(2, deviceShadow.getAvailableTags());
        ps.setString(3, deviceShadow.getDesiredTags());
        ps.setString(4, deviceShadow.getReportedTags());
        ps.executeUpdate();
        ps.close();
        return "{\"status\" : \"updated\" }";
    }

    public String updateAvailableTags(DeviceShadow deviceShadow) throws SQLException{
        TagList availableTagList = getAvailableTagsForDevice(deviceShadow.getDeviceName());
        String status = "";
        String avaiableTagListStr = "";
        for (String tag: availableTagList.getTags()) {
            avaiableTagListStr = avaiableTagListStr + tag + ",";
        }
        if(!avaiableTagListStr.contains(deviceShadow.getAvailableTags())) {
            PreparedStatement ps = connection.prepareStatement(updateAvailableTags);
            ps.setString(2, deviceShadow.getDeviceName());
            String setStr = avaiableTagListStr +
                    deviceShadow.getAvailableTags();
            ps.setString(1, setStr);
            ps.executeUpdate();
            ps.close();
            status = "{\"status\" : \"updated\"}";
        }else {
            status = "{\"error\" : \"Duplicate tag\"}";
        }
        return status;
    }

    public TagList getAvailableTagsForDevice(String device) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(fetchAvailableTags);
        ps.setString(1,device);
        ResultSet resultSet = ps.executeQuery();
        TagList tagList = new TagList();
        tagList.setDeviceName(device);
        if (resultSet.next()){
            String availableTags = resultSet.getString(1);//.replaceAll("/\\[.*?\\]/","");
            String[] list = availableTags.split(",");
            List<String> availableTagsList = Arrays.asList(list);
            //List<String> availableTagsList = new ArrayList<String>(Arrays.asList(availableTags.split(",")));
            tagList.setTags(availableTagsList);
        }
        return tagList;
    }

    public void updateDeviceState(TagList tagList, String tagType) throws  SQLException{
        logger.debug("tagType" + tagType);
        logger.debug("tagList" + tagList.getTags());
        PreparedStatement ps = null;
        if(tagType.contentEquals("desired"))
            ps = connection.prepareStatement(updateDesiredTags);
        else if(tagType.contentEquals("reported"))
            ps = connection.prepareStatement(updateReportedTags);
        String tags = "";
        for (String tag : tagList.getTags()) {
            tags = tags + tag + ",";
        }
        tags = tags.substring(0,tags.length() - 1);
        ps.setString(1, tags);
        ps.setString(2, tagList.getDeviceName());
        ps.executeUpdate();
        ps.close();
    }

    public boolean ifDeviceExists(String deviceName) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(checkDeviceExistence);
        ps.setString(1, deviceName);
        ResultSet resultSet = ps.executeQuery();
        if (resultSet.next()) {
            if(resultSet.getInt(1) > 0){
                logger.debug("Device already present");
                return true;
            }
        }
        logger.debug("Device Initially not present");
        return false;
    }

    public DeviceShadow getDeviceShadow(String device) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(getDeviceShadow);
        DeviceShadow deviceShadow = new DeviceShadow(device,"","","");
        ps.setString(1,device);
        ResultSet resultSet = ps.executeQuery();
        if (resultSet.next()){
            String availableTags = resultSet.getString(2);
            String reportedTags = resultSet.getString(3);
            String desiredTags = resultSet.getString(4);
            deviceShadow.setDeviceName(device);
            deviceShadow.setAvailableTags(availableTags);
            deviceShadow.setDesiredTags(desiredTags);
            deviceShadow.setReportedTags(reportedTags);
        }
        return deviceShadow;
    }
    
}
