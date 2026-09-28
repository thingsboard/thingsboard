// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.common.data.trendz;

import java.io.Serializable;

public record TrendzConfiguration(String trendzUrl, String tbUrl) implements Serializable {}
