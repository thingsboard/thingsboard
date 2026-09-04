// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImageDescriptor {
    private String mediaType;
    private int width;
    private int height;
    private long size;
    private String etag;
    private ImageDescriptor previewDescriptor;
}
