/**
 * Copyright © 2016-2025 The Thingsboard Authors
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
package org.thingsboard.server.common.data.job;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString(callSuper = true)
public class DummyJobConfiguration extends JobConfiguration {

    private long taskProcessingTimeMs;
    private int successfulTasksCount;
    private int failedTasksCount;
    private int permanentlyFailedTasksCount;
    private List<String> errors;
    private int retries;

    private String generalError;
    private int submittedTasksBeforeGeneralError;

    @Override
    public JobType getType() {
        return JobType.DUMMY;
    }

}
