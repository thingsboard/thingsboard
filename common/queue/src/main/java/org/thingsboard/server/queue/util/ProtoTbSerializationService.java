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

import com.google.protobuf.InvalidProtocolBufferException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.TbSerializable;
import org.thingsboard.server.common.mapping.TbSerializationMapping;
import org.thingsboard.server.common.mapping.TbSerializationRegistry;
import org.thingsboard.server.common.mapping.ToDataMapper;
import org.thingsboard.server.common.mapping.ToProtoMapper;
import org.thingsboard.server.common.msg.plugin.ComponentLifecycleMsg;
import org.thingsboard.server.gen.data.ComponentLifecycleMsgProto;
import org.thingsboard.server.gen.data.DeviceProto;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
public class ProtoTbSerializationService implements TbSerializationService {

    @Override
    public <T extends TbSerializable> Optional<T> decode(byte[] bytes, Class<T> clazz) {
        var m = (TbSerializationMapping<T>) TbSerializationRegistry.get(clazz);
        if (m == null) {
            throw new RuntimeException("Deserialization of " + clazz.getName() + " is not supported!");
        }
        try {
            return Optional.of(m.fromBytes(bytes));
        } catch (InvalidProtocolBufferException e) {
            if (log.isTraceEnabled()) {
                log.trace("Failed to decode {} from bytes {}", clazz.getName(), Arrays.toString(bytes));
            }
            return Optional.empty();
        }
    }

    @Override
    public <T extends TbSerializable> byte[] encode(T msg) {
        TbSerializationMapping<T> m = TbSerializationRegistry.get(msg);
        if (m == null) {
            throw new RuntimeException("Serialization of " + msg.getClass().getName() + " is not supported!");
        }
        return m.toBytes(msg);
    }

}
