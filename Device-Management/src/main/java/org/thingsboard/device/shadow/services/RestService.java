package org.thingsboard.device.shadow.services;


import java.io.IOException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thingsboard.device.shadow.models.TagList;

@Service("restService")
public class RestService {
    Logger logger = LoggerFactory.getLogger(RestService.class);
    private String getOpcDataUrl = "http://localhost:8088/contentListener";

    public boolean postToGetOPCData(TagList tagList) throws IOException, RuntimeException {

        String tagsStr = createTagStr(tagList);
        Boolean status = true;
        logger.debug("opca msg\n" + tagsStr);
        HttpPost postRequest = new HttpPost(getOpcDataUrl);
        StringEntity input = new StringEntity(tagsStr);
        input.setContentType("text/plain");
        postRequest.setEntity(input);

        HttpClient httpClient = HttpClientBuilder.create().build();
        HttpResponse response = httpClient.execute(postRequest);

        if (response.getStatusLine().getStatusCode() != 200) {
            status = false;
        }
        return status;
    }

    public String createTagStr(TagList tagList){
        String tagsStr = tagList.getDeviceName() + "\n";
        for (int i = 0; i < tagList.getTags().size(); i++){
            tagsStr = tagsStr + tagList.getDeviceName() + "." + tagList.getTags().get(i) + "\n";
        }
        return tagsStr;
    }
}
