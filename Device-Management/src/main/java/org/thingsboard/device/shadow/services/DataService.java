package org.thingsboard.device.shadow.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thingsboard.device.shadow.dao.DeviceShadowDao;
import org.thingsboard.device.shadow.models.DeviceShadow;
import org.thingsboard.device.shadow.models.TagList;

import java.sql.SQLException;
import java.util.*;
import java.util.List;



@Service("dataService")
public class DataService {

    private  Logger logger = LoggerFactory.getLogger(DataService.class);
    //DeviceShadowDao shadowDao = new DeviceShadowDaoImp();
    @Autowired
    private DeviceShadowDao shadowDao;
    @Autowired
    private RestService restService;
    //private RestService restService = new RestService();

    public String updateAvailableTags(JSONObject availableTags){
        JSONParser parser = new JSONParser();
        String availableTagList = null;
        String deviceName = "";
        String status = "";
        try {
            deviceName = availableTags.get("deviceName").toString();
        }catch (Exception e){
            logger.error("Exception is : " + e);
        }
        if (!availableTags.get("tags").toString().contentEquals("")) {
            //Spliting the tagList.
            availableTagList = availableTags.get("tags").toString();
            String desiredTagList = "";
            String reportedTagList = "";
            try {
                    DeviceShadow deviceShadow = new DeviceShadow(deviceName, availableTagList, desiredTagList, reportedTagList);
                    if(!shadowDao.ifDeviceExists(deviceName))
                        status = shadowDao.insertAvailableTags(deviceShadow);
                    else {
                        status = shadowDao.updateAvailableTags(deviceShadow);
                    }
            } catch (SQLException e) {
                logger.error("Error updating availableTags : " + e);
                status = "{\"error\" : \"" + e + "\"}";
            }
        }
        return status;
    }

    public void desiredTags(TagList tagList)throws Exception{
        shadowDao.updateDeviceState(tagList, "desired");
    }
    public String getAvailableTagsByDevice(String device){
        TagList tagList = null;
        String jsontTagList = "";
        try {
            tagList = shadowDao.getAvailableTagsForDevice(device);
            ObjectMapper mapper = new ObjectMapper();
            jsontTagList = mapper.writeValueAsString(tagList);
        }catch (Exception e){
            logger.error("Error in getting available tags : " + e);
            return "{\"error\": \"" + e + "\"}";
        }
        //JSONObject tagListJson = new JSONObject(tagList.getClass());
        return jsontTagList;
    }

    public String getDeviceShadow(String device){
        String jsonShadow = "";
        try {
            DeviceShadow deviceShadow = shadowDao.getDeviceShadow(device);
            ObjectMapper mapper = new ObjectMapper();
            jsonShadow = mapper.writeValueAsString(deviceShadow);
        }catch (Exception e){
            logger.error("Error in getting device shadow : " + e);
        }
        return jsonShadow;
    }

    public void updateReportedTags(JSONObject jsonObject){
        try {
            Set<String> keys = jsonObject.keySet();
            for (String key: keys) {
                TagList tagList = new TagList();
                tagList.setDeviceName(key);
                JSONArray tags = (JSONArray)jsonObject.get(key);
                List<String> reportedTags = new ArrayList<>();
                for (Object tag:tags) {
                    reportedTags.add(tag.toString());
                }
                //Update reported tags.
                tagList.setTags(reportedTags);
                shadowDao.updateDeviceState(tagList, "reported");
                logger.error("reportedTags" + reportedTags);
            }

        }catch (Exception e){
            logger.error("Exception updating tags : " + e);
        }
    }

}
