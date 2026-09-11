// SPDX-FileCopyrightText: Copyright The Thingsboard Authors
// SPDX-License-Identifier: Apache-2.0
package org.thingsboard.server.service.solutions.data;

import lombok.Data;

@Data
public class UserCredentialsInfo {

    String name;
    String login;
    String password;
    String customerName;

}
