package org.thingsboard.device.shadow.controllers;

/**
 * Created by himanshu on 29/9/17.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.thingsboard.device.shadow.models.TagList;
import org.thingsboard.device.shadow.services.DataService;
import org.thingsboard.device.shadow.services.RestService;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicLong;


@RestController
public class ShadowController {

    private final AtomicLong counter = new AtomicLong();
    //@Autowired
    private DataService dataService = new DataService();
    RestService restService = new RestService();

    @RequestMapping(value = "/update/available/tags", method = RequestMethod.POST)
    public void updateAvaliableTags(@RequestBody String availableTags){
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = null;
        JSONArray tagList = null;
        try {
            jsonObject = (JSONObject) parser.parse(availableTags);
            dataService.updateAvailableTags(jsonObject);
            //tagList =  (JSONArray) parser.parse(jsonObject.get("tags").toString());
        }catch (Exception e){

        }
    }


    @RequestMapping(value = "/desired/tags", method = RequestMethod.POST)
    public void desiredTags(@RequestBody String newTags){
        ObjectMapper mapper = new ObjectMapper();
        try {
            TagList tagList = mapper.readValue(newTags, TagList.class);
            dataService.desiredTags(tagList);
            restService.postToGetOPCData(tagList);
            //restService.postToopcuaServiceUrl(newTags);
        }catch (Exception e){

        }
    }

    @RequestMapping(value = "/get/available/tags", produces = "application/json")
    public String getAvailableTagsByToken(@RequestParam(value="token") String token) {
        String jsonTagList = dataService.getAvailableTagsBytoken(token);
        return jsonTagList;
    }

    @RequestMapping(value = "/available/tags", method = RequestMethod.DELETE)
    public void deleteTagsByToken(@RequestParam(value="token") String token) throws SQLException{
        dataService.deleteById(token);
    }

}

