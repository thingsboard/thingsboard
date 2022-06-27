/**
 * Copyright © 2016-2022 The Thingsboard Authors
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
package org.thingsboard.server.common.data;

import lombok.extern.slf4j.Slf4j;
import org.nustaq.serialization.FSTConfiguration;

@Slf4j
public class FSTUtils {

    public static final FSTConfiguration CONFIG = FSTConfiguration.createDefaultConfiguration();

    @SuppressWarnings("unchecked")
    public static <T> T decode(byte[] byteArray) {
        return byteArray != null && byteArray.length > 0 ? (T) CONFIG.asObject(byteArray) : null;
    }

    public static <T> byte[] encode(T msq) {
        return CONFIG.asByteArray(msq);
    }

}
