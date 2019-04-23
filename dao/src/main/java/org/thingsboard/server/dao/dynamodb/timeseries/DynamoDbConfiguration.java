/**
 * Copyright © 2016-2019 The Thingsboard Authors
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
package org.thingsboard.server.dao.dynamodb.timeseries;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;

@Component
public class DynamoDbConfiguration {

    @Bean
    public AmazonDynamoDB createAmazonDynamoDbClient(
            @Value("${spring.dynamodb.access.id}") String accessId,
            @Value("${spring.dynamodb.access.secret-key}") String accessKey,
            @Value("${spring.dynamodb.access.region}") String region) {
        AmazonDynamoDBClientBuilder builder = AmazonDynamoDBClientBuilder.standard();
        builder.setRegion(region);
        builder.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessId, accessKey)));
        return builder.build();
    }

    @Bean("DynamoDbExecutor")
    public ListeningExecutorService createDynamoDbExecutor() {
        return MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
    }
}
