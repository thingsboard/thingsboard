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

import lombok.Data;
import org.thingsboard.rule.engine.api.NodeConfiguration;

@Data
public class FirebasePushNotificationNodeConfiguration implements NodeConfiguration {

    private String deviceTokenTemplate;  // Template để lấy device token
    private String titleTemplate;        // Template cho tiêu đề notification
    private String bodyTemplate;         // Template cho nội dung notification
    private String serviceAccountKeyPath; // Đường dẫn đến file service account key
    private boolean useSystemFCMSetting; //Sử dụng FCM settings mặc định của hệ thống?
    private String dataTemplate;  // Template cho data (JSON string)

    @Override
    public NodeConfiguration defaultConfiguration() {
        FirebasePushNotificationNodeConfiguration configuration = new FirebasePushNotificationNodeConfiguration();
        configuration.deviceTokenTemplate = "${fmcToken}"; // Giả sử deviceToken nằm trong metadata
        configuration.titleTemplate = "Thông báo từ ThingsBoard";
        configuration.bodyTemplate = "Thiết bị có nhiệt độ cao $[temperature] hon ${threadTemperature}";
        String currentDic = System.getProperty("user.dir");
        configuration.serviceAccountKeyPath = currentDic + "/smartfarm-fa60e-firebase-adminsdk-fbsvc-9a1de21251.json"; // **CẦN THAY ĐỔI**
//        configuration.serviceAccountKeyPath = "./smartfarm-fa60e-firebase-adminsdk-fbsvc-9a1de21251.json"; // **CẦN THAY ĐỔI**
        configuration.useSystemFCMSetting = true;
        configuration.dataTemplate = "{\"key1\": \"value1\", \"key2\": \"${someValue}\"}"; // Ví dụ data JSON
        return configuration;
    }
}
