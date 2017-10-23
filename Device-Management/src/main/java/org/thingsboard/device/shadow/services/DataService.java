package org.thingsboard.device.shadow.services;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Set;


/**
 * Created by himanshu on 3/10/17.
 */

@Service("dataService")
public class DataService {

    private  Logger logger = LoggerFactory.getLogger(DataService.class);
    //DeviceShadowDao shadowDao = new DeviceShadowDaoImp();
    @Autowired
    private DeviceShadowDao shadowDao;
    @Autowired
    private RestService restService;
    //private RestService restService = new RestService();

    public void updateAvailableTags(JSONObject availableTags){
        JSONParser parser = new JSONParser();
        String tagList = null;
        String deviceToken = "";
        try {
            deviceToken = availableTags.get("token").toString();
        }catch (Exception e){
            logger.error("Exception is : " + e);
        }
        if (!availableTags.get("tags").toString().contentEquals("")) {
            //Spliting the tagList.
            tagList = availableTags.get("tags").toString();
            String[] list = tagList.split(",");
            try {
                //shadowDao.deleteByToken(deviceToken);
                for (int itr = 0; itr < list.length; itr++) {
                    DeviceShadow deviceShadow = new DeviceShadow(deviceToken, list[itr], false, false);
                    if(!shadowDao.checkIfTagExists(deviceToken, list[itr]))
                        shadowDao.updateAvailableTags(deviceShadow);
                }
            } catch (SQLException e) {
                logger.error("Error updating availableTags : " + e);
            }
        }

    }

    public void desiredTags(TagList tagList)throws Exception{
        shadowDao.updateDeviceState(tagList);
    }
    public String getAvailableTagsBytoken(String token){
        TagList tagList = null;
        String jsontTagList = "";
        try {
            tagList = shadowDao.getAllTagsForDeviceToken(token);
            ObjectMapper mapper = new ObjectMapper();
            jsontTagList = mapper.writeValueAsString(tagList);
        }catch (Exception e){
            logger.error("Error in getting available tags : " + e);
            return "{\"error\": \"" + e + "\"}";
        }
        //JSONObject tagListJson = new JSONObject(tagList.getClass());
        return jsontTagList;
    }

    public void updateReportedTags(JSONObject jsonObject){
        String reportedTags = jsonObject.get("values").toString();
        String token = jsonObject.get("token").toString();
        JSONParser parser = new JSONParser();
        JSONObject jsonObjectTagsOnly = null;
        try {
            jsonObjectTagsOnly = (JSONObject) parser.parse(reportedTags);
            Set<String> keys = jsonObjectTagsOnly.keySet();
            for (String key : keys){
                shadowDao.updateReportedTags(token, key);
            }

        }catch (Exception e){
            logger.error("Exception updating tags : " + e);
        }
    }

}
