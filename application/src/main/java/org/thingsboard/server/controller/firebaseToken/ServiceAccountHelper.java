package org.thingsboard.server.controller.firebaseToken;

import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ServiceAccountHelper {

    public static JSONObject readServiceAccountJson(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        return new JSONObject(content);
    }
}

