/**
 * Copyright © 2016 The Thingsboard Authors
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
package org.thingsboard.server.extensions.kafka;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class KafkaDemoClient {

    private static final int ZK_PORT = 2222;
    private static final String HOSTNAME = "localhost";
    private static final String ZOOKEEPER_CONNECT = HOSTNAME + ":" + ZK_PORT;
    private static final int KAFKA_PORT = 9092;
    private static final int BROKER_ID = 1;

    public static void main(String[] args) {
        try {
            startZkLocal();
            startKafkaLocal();
        } catch (Exception e) {
            System.out.println("Error running local Kafka broker");
            e.printStackTrace(System.out);
        }
    }

    private static void startZkLocal() throws Exception {
        final File zkTmpDir = File.createTempFile("zookeeper", "test");
        if (zkTmpDir.delete() && zkTmpDir.mkdir()) {
            Properties zkProperties = new Properties();
            zkProperties.setProperty("dataDir", zkTmpDir.getAbsolutePath());
            zkProperties.setProperty("clientPort", String.valueOf(ZK_PORT));

            ServerConfig configuration = new ServerConfig();
            QuorumPeerConfig quorumConfiguration = new QuorumPeerConfig();
            quorumConfiguration.parseProperties(zkProperties);
            configuration.readFrom(quorumConfiguration);

            new Thread() {
                public void run() {
                    try {
                        new ZooKeeperServerMain().runFromConfig(configuration);
                    } catch (IOException e) {
                        System.out.println("Start of Local ZooKeeper Failed");
                        e.printStackTrace(System.err);
                    }
                }
            }.start();
        } else {
            System.out.println("Failed to delete or create data dir for Zookeeper");
        }
    }

    private static void startKafkaLocal() {
        Properties kafkaProperties = new Properties();
        kafkaProperties.setProperty("host.name", HOSTNAME);
        kafkaProperties.setProperty("port", String.valueOf(KAFKA_PORT));
        kafkaProperties.setProperty("broker.id", String.valueOf(BROKER_ID));
        kafkaProperties.setProperty("zookeeper.connect", ZOOKEEPER_CONNECT);
        KafkaConfig kafkaConfig = new KafkaConfig(kafkaProperties);
        KafkaServerStartable kafka = new KafkaServerStartable(kafkaConfig);
        kafka.startup();
    }
}