// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.converter;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.converter.Converter;

public interface TbConverterService {

    Converter save(Converter entity, User user) throws Exception;

    void delete(Converter entity, User user);
}
