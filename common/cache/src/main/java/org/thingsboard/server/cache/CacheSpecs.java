// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.cache;

import lombok.Data;

@Data
public class CacheSpecs {
    private Integer timeToLiveInMinutes;
    private Integer maxSize;
}
