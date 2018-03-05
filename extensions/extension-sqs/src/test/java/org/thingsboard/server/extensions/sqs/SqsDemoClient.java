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
package org.thingsboard.server.extensions.sqs;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.Message;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Created by Valerii Sosliuk on 11/10/2017.
 */
@Slf4j
public class SqsDemoClient {

    private static final String ACCESS_KEY_ID = "$ACCES_KEY_ID";
    private static final String SECRET_ACCESS_KEY = "$SECRET_ACCESS_KEY";

    private static final String QUEUE_URL = "$QUEUE_URL";
    private static final String REGION = "us-east-1";

    public static void main(String[] args) {
        log.info("Starting SQS Demo Clinent...");
        AWSCredentials awsCredentials = new BasicAWSCredentials(ACCESS_KEY_ID, SECRET_ACCESS_KEY);
        AmazonSQS sqs = AmazonSQSClientBuilder.standard().withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .withRegion(Regions.fromName(REGION)).build();
        SqsDemoClient client = new SqsDemoClient();
        client.pollMessages(sqs);
    }

    private void pollMessages(AmazonSQS sqs) {
        log.info("Polling messages");
        while (true) {
            List<Message> messages = sqs.receiveMessage(QUEUE_URL).getMessages();
            messages.forEach(m -> {
                log.info("Message Received: " + m.getBody());
                System.out.println(m.getBody());
                DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest(QUEUE_URL, m.getReceiptHandle());
                sqs.deleteMessage(deleteMessageRequest);
            });
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                e.printStackTrace();
            }
        }
    }
}
