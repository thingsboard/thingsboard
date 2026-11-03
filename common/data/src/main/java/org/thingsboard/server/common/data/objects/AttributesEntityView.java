// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.objects;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Victor Basanets on 9/05/2017.
 */
@Data
@Schema
@NoArgsConstructor
public class AttributesEntityView implements Serializable {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of client-side attribute keys to expose", example = "[\"currentConfiguration\"]")
    private List<String> cs = new ArrayList<>();
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of server-side attribute keys to expose", example = "[\"model\"]")
    private List<String> ss = new ArrayList<>();
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "List of shared attribute keys to expose", example = "[\"targetConfiguration\"]")
    private List<String> sh = new ArrayList<>();

    public AttributesEntityView(List<String> cs,
                                List<String> ss,
                                List<String> sh) {

        this.cs = new ArrayList<>(cs);
        this.ss = new ArrayList<>(ss);
        this.sh = new ArrayList<>(sh);
    }

    public AttributesEntityView(AttributesEntityView obj) {
        this(obj.getCs(), obj.getSs(), obj.getSh());
    }
}
