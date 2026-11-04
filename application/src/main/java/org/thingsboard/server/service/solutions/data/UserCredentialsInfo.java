// SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
// SPDX-FileCopyrightText: Modifications Copyright ThingsBoard, Inc.
// SPDX-License-Identifier: Apache-2.0 AND BUSL-1.1
package org.thingsboard.server.service.solutions.data;

import lombok.Data;

@Data
public class UserCredentialsInfo {

    String name;
    String login;
    String password;
    String customerName;
    String customerGroup;


}
