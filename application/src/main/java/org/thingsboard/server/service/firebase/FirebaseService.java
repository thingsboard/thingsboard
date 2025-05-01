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
package org.thingsboard.server.service.firebase;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.*;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.TbNodeException;

import java.io.FileInputStream;


@Service
public class FirebaseService {

    private FirebaseMessaging firebaseMessaging;

    public FirebaseService() throws TbNodeException {
        try {
            String currentDic = System.getProperty("user.dir");
            FileInputStream serviceAccount =
                    new FileInputStream(currentDic + "/smartfarm-fa60e-firebase-adminsdk-fbsvc-9a1de21251.json");
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
    }

    public void sendNotification(String deviceToken, String title, String body) throws FirebaseMessagingException {
        Message message = Message.builder()
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
                        .build())
                .build();

        String response = firebaseMessaging.send(message);
        System.out.println("Sent message: " + response);
    }

}
