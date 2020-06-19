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
package org.thingsboard.server.actors;

import lombok.Getter;
import lombok.ToString;

@ToString
public class InitFailureStrategy {

    @Getter
    private boolean stop;
    @Getter
    private long retryDelay;

    private InitFailureStrategy(boolean stop, long retryDelay) {
        this.stop = stop;
        this.retryDelay = retryDelay;
    }

    public static InitFailureStrategy retryImmediately() {
        return new InitFailureStrategy(false, 0);
    }

    public static InitFailureStrategy retryWithDelay(long ms) {
        return new InitFailureStrategy(false, ms);
    }

    public static InitFailureStrategy stop() {
        return new InitFailureStrategy(true, 0);
    }
}
