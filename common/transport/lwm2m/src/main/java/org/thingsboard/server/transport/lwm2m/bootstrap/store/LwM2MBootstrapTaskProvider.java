// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.transport.lwm2m.bootstrap.store;

import org.eclipse.leshan.server.bootstrap.BootstrapTaskProvider;
import org.eclipse.leshan.server.bootstrap.InvalidConfigurationException;

public interface LwM2MBootstrapTaskProvider extends BootstrapTaskProvider {

    void put(String endpoint)  throws InvalidConfigurationException;

    void remove(String endpoint);
}
