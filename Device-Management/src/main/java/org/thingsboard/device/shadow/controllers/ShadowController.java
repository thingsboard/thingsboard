package org.thingsboard.device.shadow.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.device.shadow.models.TagList;
import org.thingsboard.device.shadow.services.DataService;
import org.thingsboard.device.shadow.services.RestService;



@RestController
public class ShadowController {

    private final Logger logger = LoggerFactory.getLogger(ShadowController.class);
    @Autowired
    private DataService dataService;
    @Autowired
    private RestService restService;

    @RequestMapping(value = "/update/available/tags", method = RequestMethod.POST)
    public String updateAvaliableTags(@RequestBody String availableTags){
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        JSONArray tagList = null;
        String status = "";
        try {
            jsonObject = (JSONObject) parser.parse(availableTags);
            logger.debug("UPDATE OBJ : \n" + jsonObject + "\n");
            status =dataService.updateAvailableTags(jsonObject);
        }catch (Exception e){
            logger.error("Exception updating tags : " + e);
            status =  "{\"error\":\""+e+"\"}";
        }
        return status;
    }

    @RequestMapping(value = "/desired/tags", method = RequestMethod.POST)
    public String desiredTags(@RequestBody String newTags){
        ObjectMapper mapper = new ObjectMapper();
        try {
            TagList tagList = mapper.readValue(newTags, TagList.class);
            //dataService.desiredTags(tagList);
            if(restService.postToGetOPCData(tagList))
                dataService.desiredTags(tagList);
        }catch (Exception e){
            logger.error("Error posting desired tags to OPC" + e);
            return "{\"error\":\""+e+"\"}";
        }
        return "{\"status\":\"updated\"}";
    }

    @RequestMapping(value = "/reported/tags", method = RequestMethod.POST)
    public String updateReportedTags(@RequestBody String reportedTags){
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;

        try {
            jsonObject = (JSONObject) parser.parse(reportedTags);
            dataService.updateReportedTags(jsonObject);
        }catch (Exception e){
            logger.error("Exception updating tags : " + e);
            return "{\"error\":\""+e+"\"}";
        }
        return "{\"status\":\"updated\"}";
    }

    @RequestMapping(value = "/available/tags", produces = "application/json", method = RequestMethod.GET)
    public String getAvailableTagsByDevice(@RequestParam(value="deviceName") String device) {
        String jsonTagList = dataService.getAvailableTagsByDevice(device);
        return jsonTagList;
    }

    @RequestMapping(value = "/device/shadow", produces = "application/json", method = RequestMethod.GET)
    public String getDeviceShadow(@RequestParam(value="deviceName") String device) {
        String jsonShadow = dataService.getDeviceShadow(device);
        return jsonShadow;
    }
}
