package org.thingsboard.server.queue.kafka;

import com.microsoft.aad.adal4j.AsymmetricKeyCredential;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class TbKafkaAdminTest {


    Properties props;
    AdminClient admin;
    @BeforeEach
    void setUp() {
        props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        admin = AdminClient.create(props);
    }

    @AfterEach
    void tearDown() {
        admin.close();
    }

    @Test
    void testListOffsets() throws ExecutionException, InterruptedException {
        log.info("Getting consumer groups list...");
        Collection<ConsumerGroupListing> consumerGroupListings = admin.listConsumerGroups().all().get();
        consumerGroupListings = consumerGroupListings.stream().sorted(Comparator.comparing(ConsumerGroupListing::groupId)).toList();
        for (ConsumerGroupListing consumerGroup : consumerGroupListings) {
            String groupId = consumerGroup.groupId();
            log.info("=== consumer group: {}", groupId);
            Map<TopicPartition, OffsetAndMetadata> consumerOffsets = admin.listConsumerGroupOffsets(groupId)
                    .partitionsToOffsetAndMetadata().get();

            // Printing the fetched offsets
            consumerOffsets.forEach((tp, om) ->log.info(tp.topic() + " partition " + tp.partition() + " offset " + om.offset()));
        }
    }

}