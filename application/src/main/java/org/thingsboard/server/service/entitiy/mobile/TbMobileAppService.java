// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.entitiy.mobile;

import org.thingsboard.server.common.data.User;
import org.thingsboard.server.common.data.mobile.app.MobileApp;

public interface TbMobileAppService {

    MobileApp save(MobileApp mobileApp, User user) throws Exception;

    void delete(MobileApp mobileApp, User user);

}
