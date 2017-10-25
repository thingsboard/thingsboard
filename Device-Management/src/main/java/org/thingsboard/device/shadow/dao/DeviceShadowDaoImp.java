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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by himanshu on 29/9/17.
 */
@Repository("deviceShadowDao")
public class DeviceShadowDaoImp implements DeviceShadowDao {

    private Logger logger = LoggerFactory.getLogger(DeviceShadowDao.class);

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
            logger.error("HMDC : Created h2 connection");
        }catch (Exception e){
            e.printStackTrace();
            logger.error("Error in creating database connection " + e);
        }
    }

    final String insertAvailableTags = "INSERT INTO device_shadow " +
            "(deviceToken,tagName,desired,reported) VALUES (?, ?, ?, ?)";

    final String fetchReportedTagsByToken = "SELECT TAGNAME from device_shadow where " +
            "DEVICETOKEN = ? and " +
            "REPORTED='TRUE'";
    final String fetchAvailableTags = "SELECT TAGNAME from device_shadow where " +
            "DEVICETOKEN = ?";

    final String insertDesiredTags = "INSERT INTO device_shadow " +
            "(deviceToken,tagName,desired,reported) VALUES (?, ?, ?, ?)";

    final String updateStateOfTag = "UPDATE device_shadow set desired='TRUE' where " +
            "DEVICETOKEN=? and TAGNAME=?";

    final String checkTagExistence = "SELECT count(*) FROM device_shadow WHERE " +
            "DEVICETOKEN=? and TAGNAME=?";

    final String updateReportedTags = "UPDATE device_shadow set reported ='TRUE' where " +
            "DEVICETOKEN=? and TAGNAME=?";

    String dummyStr = "ns=2;s=Simulation Examples.Functions.";

    public void updateAvailableTags(DeviceShadow deviceShadow) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(insertAvailableTags);
        ps.setString(1, deviceShadow.getDeviceToken());
        ps.setString(2, deviceShadow.getTagName());
        ps.setBoolean(3, deviceShadow.getDesired());
        ps.setBoolean(4, deviceShadow.getReported());
        ps.executeUpdate();
        ps.close();
    }

    public TagList getReportedTagsForDeviceToken(String token) throws SQLException{
        List<String> repotedTags = new ArrayList<String>();
        PreparedStatement ps = connection.prepareStatement(fetchReportedTagsByToken);
        ps.setString(1,token);
        ResultSet resultSet = ps.executeQuery();
        TagList tagList = new TagList();
        tagList.setToken(token);
        while (resultSet.next()){
            repotedTags.add(resultSet.getString(1));
        }
        tagList.setTags(repotedTags);
        return tagList;
    }

    public TagList getAllTagsForDeviceToken(String token) throws SQLException{
        List<String> repotedTags = new ArrayList<String>();
        PreparedStatement ps = connection.prepareStatement(fetchAvailableTags);
        ps.setString(1,token);
        ResultSet resultSet = ps.executeQuery();
        TagList tagList = new TagList();
        tagList.setToken(token);
        while (resultSet.next()){
            repotedTags.add(resultSet.getString(1));
        }
        tagList.setTags(repotedTags);
        return tagList;
    }

    public void requestNewTags(DeviceShadow deviceShadow) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(insertDesiredTags);
        ps.setString(1, deviceShadow.getDeviceToken());
        ps.setString(2, deviceShadow.getTagName());
        ps.setBoolean(3, deviceShadow.getDesired());
        ps.setBoolean(4, deviceShadow.getReported());
        ps.executeUpdate();
        ps.close();
    }

    public void updateDeviceState(TagList tagList) throws  SQLException{
        PreparedStatement ps = connection.prepareStatement(updateStateOfTag);
        for (String tag : tagList.getTags()) {
            ps.setString(1, tagList.getToken());
            ps.setString(2, tag);
            ps.executeUpdate();
        }
        ps.close();
    }

    public boolean checkIfTagExists(String token, String tag) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(checkTagExistence);
        ps.setString(1, token);
        ps.setString(2, tag);
        ResultSet resultSet = ps.executeQuery();
        if (resultSet.next()) {
            if(resultSet.getInt(1) > 0){
                logger.debug("Tag already present");
                return true;
            }
        }
        logger.debug("Tag Initially not present");
        return false;
    }

    public void updateReportedTags(String token, String tag) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(updateReportedTags);
        ps.setString(1, token);
        ps.setString(2, dummyStr+tag);
        ps.executeUpdate();
        ps.close();
    }
    
}
