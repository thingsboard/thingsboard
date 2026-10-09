// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class EncryptionKeyId extends UUIDBased {

    @JsonCreator
    public EncryptionKeyId(@JsonProperty("id") UUID id) {
        super(id);
    }

}
