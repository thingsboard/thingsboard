// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.report.configuration.style;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Heading {

    private String text;
    private Font font;
    private String color;
    private TextAlignment textAlignment;
    private VerticalAlignment verticalAlignment;
    private Integer height;

}
