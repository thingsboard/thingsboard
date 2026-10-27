// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.integration.api.converter;

import org.thingsboard.server.common.data.converter.Converter;

public interface TBDataConverter {

    void init(Converter configuration);

    void update(Converter configuration);

    void destroy();

    String getName();

}
