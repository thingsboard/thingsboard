// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.edqs.util;

import org.thingsboard.server.common.data.ObjectType;
import org.thingsboard.server.common.data.edqs.EdqsObject;
import org.thingsboard.server.common.data.edqs.EdqsObjectKey;

public interface EdqsMapper {

    <T extends EdqsObject> byte[] serialize(T value);

    EdqsObject deserialize(ObjectType type, byte[] bytes, boolean onlyKey);

    <T extends EdqsObject> EdqsObjectKey getKey(T object);

}
