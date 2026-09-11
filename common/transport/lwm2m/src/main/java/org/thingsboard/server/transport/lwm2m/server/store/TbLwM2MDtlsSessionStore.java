// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.server.store;


import org.thingsboard.server.transport.lwm2m.secure.TbX509DtlsSessionInfo;

public interface TbLwM2MDtlsSessionStore {

    void put(String endpoint, TbX509DtlsSessionInfo msg);

    TbX509DtlsSessionInfo get(String endpoint);

    void remove(String endpoint);

}
