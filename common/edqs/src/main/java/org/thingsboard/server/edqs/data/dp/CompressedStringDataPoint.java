/**
 * Copyright © 2016-2024 ThingsBoard, Inc.
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
package org.thingsboard.server.edqs.data.dp;

import lombok.Getter;
import lombok.SneakyThrows;
import org.thingsboard.server.common.data.kv.DataType;
import org.thingsboard.server.edqs.repo.TbBytePool;
import org.xerial.snappy.Snappy;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class CompressedStringDataPoint extends AbstractDataPoint {

    public static final int MIN_STR_SIZE_TO_COMPRESS = 512;
    @Getter
    private final byte[] value;

    public static final AtomicInteger cnt = new AtomicInteger();
    public static final AtomicLong uncompressedLength = new AtomicLong();
    public static final AtomicLong compressedLength = new AtomicLong();

    @SneakyThrows
    public CompressedStringDataPoint(long ts, String value) {
        super(ts);
        cnt.incrementAndGet();
        uncompressedLength.addAndGet(value.getBytes(StandardCharsets.UTF_8).length);
        this.value = TbBytePool.intern(Snappy.compress(value));
        compressedLength.addAndGet(this.value.length);
    }

    @Override
    public DataType getType() {
        return DataType.STRING;
    }

    @SneakyThrows
    @Override
    public String getStr() {
        return Snappy.uncompressString(value);
    }

    @SneakyThrows
    @Override
    public String valueToString() {
        return Snappy.uncompressString(value);
    }

}
