/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;
import org.thingsboard.rule.engine.api.notification.FirebaseService;
import org.thingsboard.server.common.data.id.TenantId;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

@Service
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
    public void sendMessage(TenantId tenantId, String credentials, String fcmToken, String title, String body) {
        FirebaseContext firebaseContext = contexts.asMap().compute(tenantId.toString(), (key, context) -> {
            if (context == null) {
                return new FirebaseContext(key, credentials);
            } else {
                context.check(credentials);
                return context;
            }
        });

        Message message = Message.builder()
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build())
                .setToken(fcmToken)
                .build();
        try {
            firebaseContext.getMessaging().send(message);
        } catch (FirebaseMessagingException e) {
            throw new RuntimeException("Failed to send message via FCM: " + e.getMessage(), e);
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
        }

        public void check(String credentials) {
            if (!this.credentials.equals(credentials)) {
                app.delete();
                this.credentials = credentials;
                init();
            }
        }

        public void destroy() {
            app.delete();
            app = null;
            messaging = null;
        }
    }

}
