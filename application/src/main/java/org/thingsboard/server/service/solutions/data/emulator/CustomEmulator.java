// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.emulator;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.util.Pair;

public interface CustomEmulator extends Emulator {

    Pair<Long, ObjectNode> getNextValue();

}
