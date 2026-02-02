/**
 * Copyright © 2016-2026 The Thingsboard Authors
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@Slf4j
public class JavaSerDesUtil {

    @SuppressWarnings("unchecked")
    public static <T> T decode(byte[] byteArray) {
        if (byteArray == null || byteArray.length == 0) {
            return null;
        }
        InputStream is = new ByteArrayInputStream(byteArray);
        try (ObjectInputStream ois = new ObjectInputStream(is)) {
            return (T) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            log.error("Error during deserialization", e);
            return null;
        }
    }

    public static <T> byte[] encode(T msq) {
        if (msq == null) {
            return null;
        }
        ByteArrayOutputStream boas = new ByteArrayOutputStream();
        try (ObjectOutputStream ois = new ObjectOutputStream(boas)) {
            ois.writeObject(msq);
            return boas.toByteArray();
        } catch (IOException e) {
            log.error("Error during serialization", e);
            throw new RuntimeException(e);
        }
    }
}
