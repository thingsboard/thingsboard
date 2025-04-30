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

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@ToString(callSuper = true)
public class DummyTask extends Task {

    private int number;
    private long processingTimeMs;
    private List<String> errors; // errors for each attempt
    private boolean failAlways;

    @Override
    public Object getKey() {
        return number;
    }

    @Override
    public TaskFailure toFailure(Throwable error) {
        return new DummyTaskFailure(number, failAlways, error.getMessage());
    }

    @Override
    public JobType getJobType() {
        return JobType.DUMMY;
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    @NoArgsConstructor
    public static class DummyTaskFailure extends TaskFailure {

        private int number;
        private boolean failAlways;

        public DummyTaskFailure(int number, boolean failAlways, String error) {
            super(error);
            this.number = number;
            this.failAlways = failAlways;
        }

        @Override
        public JobType getJobType() {
            return JobType.DUMMY;
        }

    }

}
