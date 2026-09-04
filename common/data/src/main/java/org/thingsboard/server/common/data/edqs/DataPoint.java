// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.common.data.edqs;

import org.thingsboard.server.common.data.kv.DataType;

public interface DataPoint extends Comparable<DataPoint> {

    String NOT_SUPPORTED = "Not supported!";

    long getTs();

    DataType getType();

    String getStr();

    long getLong();

    double getDouble();

    boolean getBool();

    String getJson();

    String valueToString();

}
