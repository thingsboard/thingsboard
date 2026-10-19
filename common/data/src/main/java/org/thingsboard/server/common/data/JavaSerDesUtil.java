// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
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
