// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.opcua;

import lombok.Data;

@Data
public class SubscriptionTag {

    private String key;
    private String path;
    private boolean required;

}
