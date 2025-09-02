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
package org.thingsboard.server.service.notification.provider;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class DefaultFirebaseService implements FirebaseService {

    private final Cache<String, FirebaseContext> contexts = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.DAYS)
            .<String, FirebaseContext>removalListener((key, context, cause) -> {
                if (cause == RemovalCause.EXPIRED && context != null) {
                    context.destroy();
                }
            })
            .build();

    @Override
    public void sendMessage(TenantId tenantId, String credentials, String fcmToken, String title, String body,
                            Map<String, String> data, Integer badge) throws FirebaseMessagingException {
        FirebaseContext firebaseContext = contexts.asMap().compute(tenantId.toString(), (key, context) -> {
            if (context == null) {
                return new FirebaseContext(key, credentials);
            } else {
                context.check(credentials);
                return context;
            }
        });

        Aps.Builder apsConfig = Aps.builder()
                .setSound("default");
        if (badge != null) {
            apsConfig.setBadge(badge);
        }

        Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .setApnsConfig(ApnsConfig.builder()
                        .setAps(apsConfig.build())
                        .build())
                .putAllData(data)
                .build();
        try {
            firebaseContext.getMessaging().send(message);
            log.trace("[{}] Sent message for FCM token {}", tenantId, fcmToken);
        } catch (Throwable t) {
            log.debug("[{}] Failed to send message for FCM token {}", tenantId, fcmToken, t);
            throw t;
        }
    }

    public static class FirebaseContext {
        private final String key;
        private String credentials;
        private FirebaseApp app;
        @Getter
        private FirebaseMessaging messaging;

        public FirebaseContext(String key, String credentials) {
            this.key = key;
            this.credentials = credentials;
            init();
        }

        private void init() {
            FirebaseOptions options;
            try {
                options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(IOUtils.toInputStream(credentials, StandardCharsets.UTF_8)))
                        .build();
            } catch (IOException e) {
                throw new RuntimeException("Failed to process service account credentials: " + e.getMessage(), e);
            }
            try {
                app = FirebaseApp.initializeApp(options, key);
            } catch (IllegalStateException alreadyExists) { // should never normally happen
                app = FirebaseApp.getInstance(key);
            }
            try {
                messaging = FirebaseMessaging.getInstance(app);
            } catch (IllegalStateException alreadyExists) { // should never normally happen
                messaging = FirebaseMessaging.getInstance(app);
            }
            log.debug("[{}] Initialized new FirebaseContext", key);
        }

        public void check(String credentials) {
            if (!this.credentials.equals(credentials)) {
                destroy();
                this.credentials = credentials;
                init();
            } else if (app == null || messaging == null) {
                throw new IllegalStateException("Firebase app couldn't be initialized");
            }
        }

        public void destroy() {
            if (app != null) {
                app.delete();
                app = null;
            }
            messaging = null;
            log.debug("[{}] Destroyed FirebaseContext", key);
        }
    }

}
