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
package org.thingsboard.server.transport.mqtt.util.sparkplug;

/**
 * Created by nickAS21 on 13.12.22
 */

/**
 * An interface for decoding payloads.
 *
 * @param <P> the type of payload.
 */
public interface SparkplugPayloadDecoder  <P> {

    /**
     * Builds a payload from a supplied byte array.
     *
     * @param bytes the bytes representing the payload
     * @return a payload object built from the byte array
     * @throws Exception
     */
    public P buildFromByteArray(byte[] bytes) throws Exception;
}
