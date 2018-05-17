TRUNCATE thingsboard.rule_chain;
TRUNCATE thingsboard.rule_node;

-- msg_queue dataset

INSERT INTO thingsboard.msg_queue (node_id, cluster_partition, ts_partition, ts, msg)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 200, 201, null);
INSERT INTO thingsboard.msg_queue (node_id, cluster_partition, ts_partition, ts, msg)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 200, 202, null);
INSERT INTO thingsboard.msg_queue (node_id, cluster_partition, ts_partition, ts, msg)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 300, 301, null);

-- ack_queue dataset
INSERT INTO thingsboard.msg_ack_queue (node_id, cluster_partition, ts_partition, msg_id)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 300, bebaeb60-1888-11e8-bf21-65b5d5335ba9);
INSERT INTO thingsboard.msg_ack_queue (node_id, cluster_partition, ts_partition, msg_id)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 300, 12baeb60-1888-11e8-bf21-65b5d5335ba9);
INSERT INTO thingsboard.msg_ack_queue (node_id, cluster_partition, ts_partition, msg_id)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 200, 32baeb60-1888-11e8-bf21-65b5d5335ba9);

-- processed partition dataset
INSERT INTO thingsboard.processed_msg_partitions (node_id, cluster_partition, ts_partition)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 100);
INSERT INTO thingsboard.processed_msg_partitions (node_id, cluster_partition, ts_partition)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 777);
INSERT INTO thingsboard.processed_msg_partitions (node_id, cluster_partition, ts_partition)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 202, 200);