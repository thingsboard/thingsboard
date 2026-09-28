// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.aws.kinesis;

import lombok.Data;
import software.amazon.awssdk.services.kinesis.KinesisAsyncClient;
import software.amazon.kinesis.retrieval.RecordsFetcherFactory;
import software.amazon.kinesis.retrieval.RetrievalFactory;
import software.amazon.kinesis.retrieval.RetrievalSpecificConfig;
import software.amazon.kinesis.retrieval.polling.SimpleRecordsFetcherFactory;
import software.amazon.kinesis.retrieval.polling.SynchronousBlockingRetrievalFactory;

import java.time.Duration;

@Data
public class KinesisPollingConfig implements RetrievalSpecificConfig {

    private final String streamName;

    private final KinesisAsyncClient kinesisClient;

    private int maxRecords = 10000;

    private Duration kinesisRequestTimeout = Duration.ofSeconds(30);

    private RecordsFetcherFactory recordsFetcherFactory = new SimpleRecordsFetcherFactory();

    @Override
    public RetrievalFactory retrievalFactory() {
        return new SynchronousBlockingRetrievalFactory(streamName, kinesisClient, recordsFetcherFactory, maxRecords, kinesisRequestTimeout, null);
    }
}
