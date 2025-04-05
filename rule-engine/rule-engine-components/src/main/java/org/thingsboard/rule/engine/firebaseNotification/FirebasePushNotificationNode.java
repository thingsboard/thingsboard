/**
 * Copyright © 2016-2025 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.firebaseNotification;

// Trong thingsboard-rule-engine/src/.../FirebasePushNotificationNode.java
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import com.google.gson.JsonParser;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.rule.engine.external.TbAbstractExternalNode;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.common.data.StringUtils; // Import StringUtils
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

// Các imports khác của ThingsBoard

@RuleNode(
        type = ComponentType.EXTERNAL, // Hoặc một type phù hợp
        name = "Firebase Push Notification",
        configClazz = FirebasePushNotificationNodeConfiguration.class,
        nodeDescription = "Gửi tin nhắn push notification qua Firebase",
        nodeDetails = "Có thể cấu hình tiêu đề và nội dung tin nhắn"
)
public class FirebasePushNotificationNode extends TbAbstractExternalNode {

    private FirebasePushNotificationNodeConfiguration config;
    private FirebaseMessaging firebaseMessaging;
    private final Gson gson = new Gson();

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, FirebasePushNotificationNodeConfiguration.class);
        // Khởi tạo Firebase Admin SDK
        try {
            FileInputStream serviceAccount =
                    new FileInputStream(config.getServiceAccountKeyPath());
            FirebaseOptions options = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if(FirebaseApp.getApps().isEmpty()){
                FirebaseApp.initializeApp(options);
            }
            firebaseMessaging = FirebaseMessaging.getInstance();
        } catch (Exception e) {
            throw new TbNodeException(e);
        }

        Logger.getLogger("FirebasePushNotificationNode").info("FirebasePushNotificationNode is initialized");

    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) throws ExecutionException, InterruptedException, TbNodeException {
        try {
            String deviceToken = replaceTemplateVariables(config.getDeviceTokenTemplate(), msg);
            String title = replaceTemplateVariables(config.getTitleTemplate(), msg);
            String body = replaceTemplateVariables(config.getBodyTemplate(), msg);
            String dataJson = config.getDataTemplate();
            if (StringUtils.isEmpty(deviceToken)) {
                ctx.tellFailure(msg, new IllegalArgumentException("Device token không được tìm thấy hoặc rỗng."));
                return;
            }
            // Parse data JSON
            Map<String, String> data = null;
            if (!StringUtils.isEmpty(dataJson)) {
                try {
                    JsonObject jsonObject = gson.fromJson(dataJson, JsonObject.class);
                    data = new HashMap<>();
                    Map<String, String> finalData = data;
                    jsonObject.entrySet().forEach(entry -> finalData.put(entry.getKey(), entry.getValue().getAsString()));
                } catch (Exception e) {
                    ctx.tellFailure(msg, new IllegalArgumentException("Không thể parse data JSON: " + e.getMessage()));
                    return;
                }
            }
            Message.Builder messageBuilder = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setAndroidConfig(AndroidConfig.builder()
                            .setPriority(AndroidConfig.Priority.HIGH)
                            .setNotification(AndroidNotification.builder()
                                    .setChannelId("default_channel")
                                    .build())
                            .build());
            if (data != null) {
                messageBuilder.putAllData(data);
            }
            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            ctx.tellSuccess(msg);
            Logger.getLogger("FirebasePushNotificationNode").info("Sent message: " + response);
        }  catch (Exception e) {
            ctx.tellFailure(msg, e);
            Logger.getLogger("FirebasePushNotificationNode").severe("Failed to send message: " + e.getMessage());
        }
    }

    private String replaceTemplateVariablesMeta(String template, TbMsg msg) {
        Logger logger = Logger.getLogger("FirebasePushNotificationNode-replace");
        logger.info("Original template: " + msg.getMetaData());
        for (Map.Entry<String, String> entry : msg.getMetaData().getData().entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue());
            Logger.getLogger("FirebasePushNotificationNode-replace").info("Replace " + entry.getKey() + " with " + entry.getValue());
        }
        return template;
    }

    private String replaceTemplateVariables(String template, TbMsg msg) {
        Logger logger = Logger.getLogger("FirebasePushNotificationNode-replace");

        logger.info("Original template: " + template);
        logger.info("Payload: " + msg.getData());

        // Parse msg.getData() từ JSON string sang JSON object
        JsonObject jsonData = JsonParser.parseString(msg.getData()).getAsJsonObject();

        for (String key : jsonData.keySet()) {
            String value = jsonData.get(key).getAsString(); // Lấy giá trị từ payload
            template = template.replace("$[" + key + "]", value);
            logger.info("Replace $" + key + " with " + value);
        }

        logger.info("Final template: " + template);
        return replaceTemplateVariablesMeta(template, msg);
    }


    @Override
    public void destroy() {
        // Giải phóng tài nguyên nếu cần
    }
}