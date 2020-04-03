/**
 * Copyright © 2016-2020 The Thingsboard Authors
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
package org.thingsboard.server.queue.eventhub;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

@Slf4j
@ConditionalOnExpression("'${queue.type:null}'=='eventhubs'")
@Component
@Data
public class TbEventHubsSettings {
    @Value("${queue.eventhubs.namespace_name:TbQueue}")
    private String namespaceName;
    @Value("${queue.eventhubs.sas_key_name:TB-queue}")
    private String sasKeyName;
    @Value("${queue.eventhubs.sas_key:H2ZtrHi1SMZRTqbf4VcT3DKhI4hWPd5LIs4GzUkkCZ0=}")
    private String sasKey;

    public String getSasToken() {
        try {
            return getSasToken(namespaceName + ".servicebus.windows.net/", sasKeyName, sasKey);
        } catch (InvalidKeyException | NoSuchAlgorithmException | UnsupportedEncodingException e) {
            log.error("Failed to create SAS Token.", e);
            throw new RuntimeException("Failed to create SAS Token.", e);
        }
    }

    private static String getSasToken(String resourceUri, String keyName, String key) throws NoSuchAlgorithmException, InvalidKeyException, UnsupportedEncodingException {
        long epoch = System.currentTimeMillis() / 1000L;
        int week = 60 * 60 * 24 * 7;
        String expiry = Long.toString(epoch + week);
        String stringToSign = URLEncoder.encode(resourceUri, "UTF-8") + "\n" + expiry;
        String signature = getHMAC256(key, stringToSign);
        return "SharedAccessSignature sr=" + URLEncoder.encode(resourceUri, "UTF-8") + "&sig=" +
                URLEncoder.encode(signature, "UTF-8") + "&se=" + expiry + "&skn=" + keyName;
    }

    public static String getHMAC256(String key, String input) throws InvalidKeyException, NoSuchAlgorithmException {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(key.getBytes(), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        Base64.Encoder encoder = Base64.getEncoder();
        return new String(encoder.encode(sha256_HMAC.doFinal(input.getBytes(StandardCharsets.UTF_8))));
    }
}
