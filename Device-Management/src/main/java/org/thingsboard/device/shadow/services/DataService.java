package org.thingsboard.device.shadow.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thingsboard.device.shadow.dao.DeviceShadowDao;
import org.thingsboard.device.shadow.dao.DeviceShadowDaoImp;
import org.thingsboard.device.shadow.models.DeviceShadow;
import org.thingsboard.device.shadow.models.TagList;

import java.sql.SQLException;


/**
 * Created by himanshu on 3/10/17.
 */
public class DataService {

    Logger logger = LoggerFactory.getLogger(DataService.class);
    DeviceShadowDao shadowDao = new DeviceShadowDaoImp();
    RestService restService = new RestService();

    public JSONObject updateAvailableTags(JSONObject availableTags) throws Exception{
        JSONParser parser = new JSONParser();
        String tagList = null;
        String deviceToken = "";

        try {
            //tagList = (JSONArray) parser.parse(availableTags.get("tags").toString());
            deviceToken = availableTags.get("token").toString();
        }catch (Exception e){

        }
        if (!availableTags.get("tags").toString().contentEquals("")) {
            //Spliting the tagList.
            tagList = availableTags.get("tags").toString();
            String[] list = tagList.split(",");
            try {
                shadowDao.deleteByToken(deviceToken);
                for (int itr = 0; itr < list.length; itr++) {
                    DeviceShadow deviceShadow = new DeviceShadow(deviceToken, list[itr], false, true);
                    shadowDao.updateAvailableTags(deviceShadow);
                }
            } catch (Exception SQLException) {

            }
        }
        else {
            try {
                shadowDao.deleteByToken(deviceToken);
            }catch (SQLException e){

            }
        }
        //DeviceShadow deviceShadow = new DeviceShadow();
        return availableTags;
    }

    public void desiredTags(TagList tagList)throws Exception{
        logger.error("here I am! desired tags");
        shadowDao.updateDeviceState(tagList);
    }
    public String getAvailableTagsBytoken(String token){
        TagList tagList = null;
        String jsontTagList = "";
        try {
            tagList = shadowDao.getReportedTagsForDeviceToken(token);
            ObjectMapper mapper = new ObjectMapper();
            jsontTagList = mapper.writeValueAsString(tagList);
        }catch (Exception e){

        }
        //JSONObject tagListJson = new JSONObject(tagList.getClass());
        return jsontTagList;
    }

    public void deleteById(String token)throws SQLException{
        if(shadowDao.deleteByToken(token)){
            try {
                restService.postToThingsBoard(token);
            }catch (Exception e){

            }
        }
    }

}
