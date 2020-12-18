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
package org.thingsboard.server.service.edge.rpc;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class EdgeEventStorageSettings {
    @Value("${edges.storage.max_read_records_count}")
    private int maxReadRecordsCount;
    @Value("${edges.storage.no_read_records_sleep}")
    private long noRecordsSleepInterval;
    @Value("${edges.storage.sleep_between_batches}")
    private long sleepIntervalBetweenBatches;
}
