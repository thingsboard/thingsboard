// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;

import org.eclipse.leshan.server.security.NonUniqueSecurityInfoException;
import org.thingsboard.server.transport.lwm2m.secure.TbLwM2MSecurityInfo;

public interface TbEditableSecurityStore extends TbSecurityStore {

    void put(TbLwM2MSecurityInfo tbSecurityInfo) throws NonUniqueSecurityInfoException;

    void remove(String endpoint);

}
