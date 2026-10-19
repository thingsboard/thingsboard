// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.id;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class CustomMenuId extends UUIDBased {

    @JsonCreator
    public CustomMenuId(@JsonProperty("id") UUID id){
        super(id);
    }
    
}
