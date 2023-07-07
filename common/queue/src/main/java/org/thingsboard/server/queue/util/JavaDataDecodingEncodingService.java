/**
 * Copyright © 2016-2023 The Thingsboard Authors
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
package org.thingsboard.server.queue.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.JavaSerDesUtil;

import java.util.Optional;

@Slf4j
@Service
public class JavaDataDecodingEncodingService implements DataDecodingEncodingService {
    @Override
    public <T> Optional<T> decode(byte[] byteArray) {
        return Optional.ofNullable(JavaSerDesUtil.decode(byteArray));
    }

    @Override
    public <T> byte[] encode(T msq) {
        return JavaSerDesUtil.encode(msq);
    }
}
