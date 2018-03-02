CREATE TABLE IF NOT EXISTS thingsboard.msg_queue (
    node_id         timeuuid,
    clustered_hash    bigint,
    partition       bigint,
    ts              bigint,
    msg             blob,
	PRIMARY KEY ((node_id, clustered_hash, partition), ts))
WITH CLUSTERING ORDER BY (ts DESC)
AND compaction = {
    'class': 'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy',
    'min_threshold': '5',
    'base_time_seconds': '43200',
    'max_window_size_seconds': '43200',
    'tombstone_threshold': '0.9',
    'unchecked_tombstone_compaction': 'true'
};


CREATE TABLE IF NOT EXISTS thingsboard.msg_ack_queue (
    node_id         timeuuid,
    clustered_hash    bigint,
    partition       bigint,
    msg_id              timeuuid,
	PRIMARY KEY ((node_id, clustered_hash, partition), msg_id))
WITH CLUSTERING ORDER BY (msg_id DESC)
AND compaction = {
    'class': 'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy',
    'min_threshold': '5',
    'base_time_seconds': '43200',
    'max_window_size_seconds': '43200',
    'tombstone_threshold': '0.9',
    'unchecked_tombstone_compaction': 'true'
};

CREATE TABLE IF NOT EXISTS thingsboard.processed_msg_partitions (
    node_id         timeuuid,
    clustered_hash    bigint,
    partition       bigint,
	PRIMARY KEY ((node_id, clustered_hash), partition))
WITH CLUSTERING ORDER BY (partition DESC)
AND compaction = {
    'class': 'org.apache.cassandra.db.compaction.DateTieredCompactionStrategy',
    'min_threshold': '5',
    'base_time_seconds': '43200',
    'max_window_size_seconds': '43200',
    'tombstone_threshold': '0.9',
    'unchecked_tombstone_compaction': 'true'
};



-- msg_queue dataset

INSERT INTO thingsboard.msg_queue (node_id, clustered_hash, partition, ts, msg)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 200, 201, null);
INSERT INTO thingsboard.msg_queue (node_id, clustered_hash, partition, ts, msg)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 200, 202, null);
INSERT INTO thingsboard.msg_queue (node_id, clustered_hash, partition, ts, msg)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 300, 301, null);

-- ack_queue dataset
INSERT INTO msg_ack_queue (node_id, clustered_hash, partition, msg_id)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 300, bebaeb60-1888-11e8-bf21-65b5d5335ba9);
INSERT INTO msg_ack_queue (node_id, clustered_hash, partition, msg_id)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 300, 12baeb60-1888-11e8-bf21-65b5d5335ba9);
    INSERT INTO msg_ack_queue (node_id, clustered_hash, partition, msg_id)
        VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 200, 32baeb60-1888-11e8-bf21-65b5d5335ba9);

-- processed partition dataset
INSERT INTO processed_msg_partitions (node_id, clustered_hash, partition)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 100);
INSERT INTO processed_msg_partitions (node_id, clustered_hash, partition)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 101, 777);
INSERT INTO processed_msg_partitions (node_id, clustered_hash, partition)
    VALUES (055eee50-1883-11e8-b380-65b5d5335ba9, 202, 200);