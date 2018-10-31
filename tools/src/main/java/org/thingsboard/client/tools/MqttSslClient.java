/**
 * Copyright © 2016-2018 The Thingsboard Authors
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
package org.thingsboard.client.tools;

/**
 * @author Valerii Sosliuk
 * This class is intended for manual MQTT SSL Testing
 */

import com.google.common.io.Resources;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.security.KeyStore;

@Slf4j
public class MqttSslClient {


    private static final String MQTT_URL = "ssl://localhost:1883";

    private static final String CLIENT_ID = "MQTT_SSL_JAVA_CLIENT";
    private static final String KEY_STORE_FILE = "mqttclient.jks";
    private static final String JKS="JKS";
    private static final String TLS="TLS";

    public static void main(String[] args) {

        try {
            URL ksUrl = Resources.getResource(KEY_STORE_FILE);
            File ksFile = new File(ksUrl.toURI());
            URL tsUrl = Resources.getResource(KEY_STORE_FILE);
            File tsFile = new File(tsUrl.toURI());
            char[] ksPwd = setKeyPassword();

            KeyStore trustStore = setKeyStore(tsFile, ksPwd);

            TrustManagerFactory tmf = setTrustManagerFactory(trustStore);

            KeyStore ks = setKeyStore2(ksFile, ksPwd);

            KeyManagerFactory kmf = setKeyManagerFactory(ks);

            SSLContext sslContext = setSSLContext(kmf, tmf);

            MqttConnectOptions mqttConnectOptions = setMqttConnectOptions(sslContext);

            MqttAsyncClient mqttAsyncClient = setMqttAsyncClient(mqttConnectOptions);

            setMessage(mqttAsyncClient, "v1/devices/me/telemetry", "{\"key1\":\"value1\", \"key2\":true, \"key3\": 3.0, \"key4\": 4}");

        } catch (Exception e) {
            log.error("Unexpected exception occurred in MqttSslClient", e);
        }
    }

    private static char[] setKeyPassword(){
        return new char[]{0x63, 0x6C, 0x69, 0x65, 0x6E, 0x74, 0x5F, 0x6B, 0x73, 0x5F, 0x70, 0x61, 0x73, 0x73, 0x77, 0x6F, 0x72, 0x64};
    }

    private static KeyStore setKeyStore(File tsFile, char[] ksPwd) throws Exception{
        KeyStore trustStore = KeyStore.getInstance(JKS);
        trustStore.load(new FileInputStream(tsFile), ksPwd);
        return trustStore;
    }

    private static TrustManagerFactory setTrustManagerFactory(KeyStore trustStore) throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(trustStore);
        return tmf;
    }

    private static KeyStore setKeyStore2(File ksFile, char[] ksPwd)throws Exception{
        KeyStore ks = KeyStore.getInstance(JKS);
        ks.load(new FileInputStream(ksFile), ksPwd);
        return ks;
    }

    private static KeyManagerFactory setKeyManagerFactory(KeyStore ks) throws Exception {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        char[] clientPwd = new char[]{0x63, 0x6C, 0x69, 0x65, 0x6E, 0x74, 0x5F, 0x6B, 0x65, 0x79, 0x5F, 0x70, 0x61, 0x73, 0x73, 0x77, 0x6F, 0x72, 0x64};
        kmf.init(ks, clientPwd);
        return kmf;
    }

    private static SSLContext setSSLContext(KeyManagerFactory kmf, TrustManagerFactory tmf) throws Exception {
        KeyManager[] km = kmf.getKeyManagers();
        TrustManager[] tm = tmf.getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance(TLS);
        sslContext.init(km, tm, null);
        return sslContext;
    }

    private static MqttConnectOptions setMqttConnectOptions(SSLContext sslContext){
        MqttConnectOptions options = new MqttConnectOptions();
        options.setSocketFactory(sslContext.getSocketFactory());
        return options;
    }

    private static MqttAsyncClient setMqttAsyncClient(MqttConnectOptions options) throws Exception{
        MqttAsyncClient client = new MqttAsyncClient(MQTT_URL, CLIENT_ID);
        client.connect(options);
        Thread.sleep(3000);
        return client;
    }

    private static void setMessage(MqttAsyncClient client, String topic, String payload) throws Exception {
        MqttMessage message = new MqttMessage();
        message.setPayload(payload.getBytes());
        client.publish(topic, message);
        client.disconnect();
        log.info("Disconnected");
        System.exit(0);
    }

}