// SPDX-FileCopyrightText: Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: BUSL-1.1
package org.thingsboard.server.service.entitiy.extension;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.extension.Extension;

public interface TbExtensionService {

    Extension save(Extension extension, User user) throws Exception;

    boolean delete(Extension extension, User user);

    Extension restart(Extension extension, User user);

    Extension stop(Extension extension, User user);

    Extension start(Extension extension, User user);

}
