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
package org.thingsboard.server.kafka;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.PartitionInfo;

import java.util.List;

/**
 * Created by ashvayka on 25.09.18.
 */
public interface TbKafkaPartitioner<T> extends Partitioner {

    int partition(String topic, String key, T value, byte[] encodedValue, List<PartitionInfo> partitions);

}
