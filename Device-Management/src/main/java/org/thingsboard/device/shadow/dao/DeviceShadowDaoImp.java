package org.thingsboard.device.shadow.dao;

import org.apache.tomcat.jdbc.pool.DataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.thingsboard.device.shadow.models.DeviceShadow;
import org.thingsboard.device.shadow.models.TagList;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by himanshu on 29/9/17.
 */

public class DeviceShadowDaoImp implements DeviceShadowDao {

    /*public void updateAvailableTags(DeviceShadow deviceShadow){
        deviceShadowDao.save(deviceShadow);
    }*/
    Connection connection = null;
    @Autowired
    DataSource dataSource;

    public DeviceShadowDaoImp(){
        try {
            String JDBC_DRIVER = "org.h2.Driver";
            String DB_URL = "jdbc:h2:file:~/test2";
            Class.forName(JDBC_DRIVER);
            String USER = "sa";
            String PASS = "";
            connection = DriverManager.getConnection(DB_URL,USER,PASS);
        }catch (Exception e){

        }
    }

    final String insertReportedTags = "INSERT INTO device_shadow " +
            "(deviceToken,tagName,desired,reported) VALUES (?, ?, ?, ?)";

    final String fetchReportedTagsByToken = "SELECT TAGNAME from device_shadow where " +
            "DEVICETOKEN = ? and " +
            "REPORTED='TRUE'";

    final String insertDesiredTags = "INSERT INTO device_shadow " +
            "(deviceToken,tagName,desired,reported) VALUES (?, ?, ?, ?)";

    final String updateStateOfTag = "UPDATE device_shadow set desired='TRUE' where " +
            "DEVICETOKEN=? and TAGNAME=?";

    final String deleteByToken = "delete from device_shadow where DEVICETOKEN=?";

    public void updateByDeviceToken(String token){

    }

    public boolean deleteByToken(String deviceToken){
        try {
            PreparedStatement ps = connection.prepareStatement(deleteByToken);
            ps.setString(1, deviceToken);
            ps.execute();
            ps.close();
        }catch (SQLException e){
            return false;
        }
        return true;
    }

    public void updateAvailableTags(DeviceShadow deviceShadow) throws SQLException{
        PreparedStatement ps = connection.prepareStatement(insertReportedTags);
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
}
