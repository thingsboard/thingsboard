// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.notification.targets.slack;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SlackFile {

    private final String name;
    private final String type; // one of https://api.slack.com/types/file#file_types
    private final byte[] data;

}
