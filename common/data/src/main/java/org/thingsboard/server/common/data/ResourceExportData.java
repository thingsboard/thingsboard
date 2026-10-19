// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Schema
@Slf4j
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResourceExportData {

    private String link;
    private String title;
    private ResourceType type;
    private ResourceSubType subType;
    private String resourceKey;
    private String fileName;
    private String publicResourceKey;
    private boolean isPublic;
    private String mediaType;
    private String data;

    /*
     * when importing resource, the previous link may be changed due to existing duplicates or something else.
     * this is the new proper link to be used in place of the old link
     * */
    @JsonIgnore
    private String newLink;

}
