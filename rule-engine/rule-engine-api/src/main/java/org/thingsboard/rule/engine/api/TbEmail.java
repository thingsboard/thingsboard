// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.rule.engine.api;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TbEmail {

    private final String from;
    private final String to;
    private final String cc;
    private final String bcc;
    private final String subject;
    private final String body;
    private final Map<String, String> images;
    private final boolean html;

}
