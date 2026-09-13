// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.cf;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.thingsboard.server.common.data.id.CalculatedFieldId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.msg.TbMsg;

import java.util.List;
import java.util.Objects;

public interface CalculatedFieldResult {

    TbMsg toTbMsg(EntityId entityId, String cfName, List<CalculatedFieldId> cfIds);

    String stringValue();

    boolean isEmpty();

    default JsonElement toJsonElement() {
        return JsonParser.parseString(Objects.requireNonNull(stringValue()));
    }

}
