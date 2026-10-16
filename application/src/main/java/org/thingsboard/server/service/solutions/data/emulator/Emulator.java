// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.emulator;

import org.thingsboard.server.service.solutions.data.definition.EmulatorDefinition;

public interface Emulator {

    void init(EmulatorDefinition emulatorDefinition);

}
