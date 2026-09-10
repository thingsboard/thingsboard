// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data.names;

import lombok.Data;

@Data
public class RandomNameData {

    private final String firstName;
    private final String lastName;
    private final String email;
}
