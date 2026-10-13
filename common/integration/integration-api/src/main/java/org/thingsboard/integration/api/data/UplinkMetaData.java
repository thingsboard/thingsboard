// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.data;

import lombok.Data;

import java.util.Map;

@Data
public class UplinkMetaData<T> {

    private final ContentType contentType;

    private final Map<String, T> kvMap;

}
